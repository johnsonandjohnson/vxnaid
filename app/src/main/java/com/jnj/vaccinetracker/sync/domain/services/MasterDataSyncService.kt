package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.helpers.ServerPollUtil
import com.jnj.vaccinetracker.sync.data.models.MasterDataUpdateEntryDto
import com.jnj.vaccinetracker.sync.data.models.MasterDataUpdatesResponse
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.entities.MasterSyncStatus
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.factories.SyncMasterDataUseCaseFactory
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.GetLocalMasterDataModifiedUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.masterdata.GetMasterDataHashUseCase
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * to be used in foreground service
 */
@Singleton
class MasterDataSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val api: VaccineTrackerSyncApiDataSource,
    private val networkConnectivity: NetworkConnectivity,
    private val syncMasterDataUseCaseFactory: SyncMasterDataUseCaseFactory,
    private val getLocalMasterDataModifiedUseCase: GetLocalMasterDataModifiedUseCase,
    private val getMasterDataHashUseCase: GetMasterDataHashUseCase,
    private val syncSettingsObserver: SyncSettingsObserver,
    private val serverPollUtil: ServerPollUtil,
    private val syncLogger: SyncLogger,
) {

    private val MasterDataFile.localDateModified: SyncDate? get() = getLocalMasterDataModifiedUseCase.getMasterDataSyncDate(this)

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var pollServerJob: Job? = null

    companion object {
        private val counter = Counters.MasterDataSync
    }

    fun start() {
        if (pollServerJob?.isActive != true) {
            pollServerJob = scope.launch(dispatchers.io) {
                pollServerPeriodically()
            }
        }
    }

    private suspend fun MasterDataFile.calcSyncStatus(updateEntry: MasterDataUpdateEntryDto?): MasterSyncStatus {
        val masterDataFile = this
        logInfo("calcSyncStatus $masterDataFile $updateEntry")

        if (updateEntry?.dateModified != null) {
            val localDateModified = masterDataFile.localDateModified
            logInfo("calcSyncStatus $masterDataFile localDateModified:$localDateModified")
            return when (localDateModified) {
                null -> MasterSyncStatus.EMPTY
                updateEntry.dateModified -> MasterSyncStatus.OK
                else -> MasterSyncStatus.STALE
            }
        }
        if (updateEntry?.hash != null) {
            val localHash = getMasterDataHashUseCase.getMasterDataHash(masterDataFile)
            logInfo("calcSyncStatus $masterDataFile localHash:$localHash")
            return when (localHash) {
                null -> MasterSyncStatus.EMPTY
                updateEntry.hash -> MasterSyncStatus.OK
                else -> MasterSyncStatus.STALE
            }
        }
        val defaultMasterSyncStatus = MasterSyncStatus.EMPTY
        logWarn("calcSyncStatus invalid MasterDataUpdateEntryDto, both hash and dateModified are null $masterDataFile $updateEntry $defaultMasterSyncStatus")
        return defaultMasterSyncStatus
    }

    private suspend fun storeIfOutOfSync(masterDataFile: MasterDataFile, updateEntry: MasterDataUpdateEntryDto?) {
        suspend fun calcSyncStatus() = masterDataFile.calcSyncStatus(updateEntry).also {
            syncLogger.logMasterSyncStatus(masterDataFile, it, SyncDate(dateNow()))
        }

        val syncStatus = calcSyncStatus()
        logInfo("storeIfOutOfSync $syncStatus $masterDataFile $updateEntry")
        if (syncStatus == MasterSyncStatus.OK) {
            return
        }
        val useCase = syncMasterDataUseCaseFactory.create(masterDataFile)
        networkConnectivity.awaitFastInternet(debugLabel())
        syncSettingsObserver.awaitSyncCredentialsAvailable(debugLabel())
        val dateModified = updateEntry?.dateModified ?: SyncDate(dateNow())
        try {
            useCase.sync(dateModified).also {
                //update sync date
                calcSyncStatus()
                //notify get master data use case that it should update memory cache
                syncLogger.logMasterDataPersisted(masterDataFile)
            }
        } catch (ex: Throwable) {
            if (!networkConnectivity.isConnectedAccurate()) {
                logWarn("error occurred trying to sync due to bad internet: $masterDataFile", ex)
                return storeIfOutOfSync(masterDataFile, updateEntry)
            } else
                logError("error occurred trying to sync: $masterDataFile", ex)
            // can't rely on network connectivity state
            // in case we don't have file stored for this master data then wait for a short while and try again
            if (syncStatus == MasterSyncStatus.EMPTY) {
                val delay = counter.FIRST_INIT_ERROR_RETRY_DELAY
                logInfo("We don't have an existing $masterDataFile, try again after $delay ms")
                delaySafe(delay)
                return storeIfOutOfSync(masterDataFile, updateEntry)
            }

        }
    }

    private fun CoroutineScope.launchDownloadTask(masterDataFile: MasterDataFile, entry: MasterDataUpdateEntryDto?) = launch {
        storeIfOutOfSync(masterDataFile, entry)
    }


    private suspend fun storeData(masterDataUpdatesResponse: MasterDataUpdatesResponse) = coroutineScope {
        MasterDataFile.values().forEach { masterDataFile ->
            // the coroutineScope will wait until all launched jobs are completed.
            launchDownloadTask(masterDataFile, masterDataUpdatesResponse.find { masterDataFile.syncName == it.name })
        }
    }

    private suspend fun fetchUpdates(): MasterDataUpdatesResponse {
        return api.getMasterDataUpdates()
    }

    private suspend fun doSync() {
        logInfo("pollServer")
        networkConnectivity.awaitFastInternet(debugLabel())
        syncSettingsObserver.awaitSyncCredentialsAvailable(debugLabel())
        val syncErrorMetadata = SyncErrorMetadata.MasterDataUpdatesCall()
        val updates = try {
            fetchUpdates().also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (ex: NoNetworkException) {
            logWarn("no network to poll server for master data updates, trying again")
            return doSync()
        } catch (ex: Throwable) {
            if (!networkConnectivity.isConnectedAccurate()) {
                logWarn("no network to poll server for master data updates, trying again", ex)
                return doSync()
            }
            syncLogger.logSyncError(syncErrorMetadata, ex)
            logError("something went wrong fetch master data updates", ex)
            return
        }

        storeData(updates)
    }

    private suspend fun pollServer() {
        try {
            syncLogger.logMasterDataSyncInProgress(true)
            doSync()
        } finally {
            syncLogger.logMasterDataSyncInProgress(false)
        }
    }

    private suspend fun pollServerPeriodically() {
        serverPollUtil.pollServerPeriodically(delayMs = counter.DELAY,
            debugLabel = debugLabel(), skipDelayWhenSyncCredentialsChanged = true, skipDelayWhenSyncSettingsChanged = false)
        { pollServer(); true }
    }

    fun cancel() {
        scope.cancel()
    }
}