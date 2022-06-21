package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.domain.usecases.ChangeDeviceNameUseCase
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DeviceNameSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val changeDeviceNameUseCase: ChangeDeviceNameUseCase,
    private val networkConnectivity: NetworkConnectivity,
    private val syncSettingsObserver: SyncSettingsObserver,
    private val syncLogger: SyncLogger,
) {

    companion object {
        private val counter = Counters.ChangeDeviceName
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)


    private var deviceNameObserveJob: Job? = null

    fun start() {
        startObserveDeviceNameJob()
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeSiteUuid() {
        syncSettingsObserver.observeSiteSelectionChanges()
            .collectLatest {
                logInfo("Observe site UUID is changed")
                changeDeviceName(it, counter.RETRY_COUNT)
            }
    }

    private fun startObserveDeviceNameJob() {
        if (deviceNameObserveJob?.isActive != true) {
            scope.launch {
                observeSiteUuid()
            }.also {
                deviceNameObserveJob = it
            }
        }
    }


    private suspend fun changeDeviceName(siteUuid: String, retries: Int) {
        logInfo("changeDeviceName await fast internet")
        networkConnectivity.awaitFastInternet(debugLabel())
        syncSettingsObserver.awaitSyncCredentialsAvailable(debugLabel())
        val syncErrorMetadata = SyncErrorMetadata.DeviceNameCall(siteUuid)
        try {
            changeDeviceNameUseCase.changeDeviceName(siteUuid)
            syncLogger.clearSyncError(syncErrorMetadata)
        } catch (ex: NoNetworkException) {
            logWarn("no network to query server for device name, trying again")
            return changeDeviceName(siteUuid, retries)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            if (!networkConnectivity.isConnectedAccurate()) {
                logWarn("no network to query server for device name, trying again", ex)
                return changeDeviceName(siteUuid, retries)
            } else if (retries > 0) {
                logError("something went wrong trying to update device name", ex)
                delay(counter.SHORT_RETRY_DELAY)
                return changeDeviceName(siteUuid, retries - 1)
            }

            syncLogger.logSyncError(syncErrorMetadata, ex)
            delaySafe(counter.LONG_RETRY_DELAY)
            return changeDeviceName(siteUuid, counter.RETRY_COUNT)
        }
    }
}