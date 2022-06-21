package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.usecases.error.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject
import javax.inject.Singleton

/**
 * must be used in Android foreground service
 */
@Singleton
class SyncErrorSyncService @Inject constructor(
    private val networkConnectivity: NetworkConnectivity,
    private val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsObserver: SyncSettingsObserver,
    private val observeSyncErrorsUseCase: ObserveSyncErrorsUseCase,
    private val findAllSyncErrorsPendingUploadUseCase: FindAllSyncErrorsPendingUploadUseCase,
    private val uploadSyncErrorsUseCase: UploadSyncErrorsUseCase,
    private val uploadResolvedSyncErrorKeysUseCase: UploadResolvedSyncErrorKeysUseCase,
    private val findAllSyncErrorKeysResolvedUseCase: FindAllSyncErrorKeysResolvedUseCase,
) {

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)


    private var uploadJob: Job? = null
    private var errorObservableJob: Job? = null

    companion object {
        val counter = Counters.UpstreamSyncErrorSync
        private const val BATCH_SIZE_SYNC_ERRORS = 25
        private const val BATCH_SIZE_SYNC_ERROR_KEYS = 50
    }


    fun startUploadingErrors() {
        startObserveErrorsJob()
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeErrors() {
        observeSyncErrorsUseCase.observeChanges()
            .debounce(counter.OBSERVE_ERRORS_DEBOUNCE)
            .collectLatest {
                logInfo("observeDraftChanges collect")
                uploadJob?.join()
                startUploadJob()
            }
    }

    private fun startObserveErrorsJob() {
        if (errorObservableJob?.isActive != true) {
            scope.launch {
                observeErrors()
            }.also {
                errorObservableJob = it
            }
        }
    }

    private fun startUploadJob() {
        logInfo("startUploadJob")
        if (uploadJob?.isActive != true) {
            scope.launch(dispatchers.io) {
                upload()
            }.also {
                uploadJob = it
            }
        }
    }

    private suspend fun upload() {
        logInfo("upload")
        try {
            networkConnectivity.awaitFastInternet(debugLabel())
            syncSettingsObserver.awaitSyncSettingsAvailable(debugLabel())
            uploadAllSyncErrorsPendingUpload()
            uploadAllSyncErrorsResolved()
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("upload error", ex)
            if (networkConnectivity.isConnectedFast() && syncSettingsObserver.isSyncSettingsAvailable())
                delaySafe(counter.RETRY_DELAY)
            upload()
        }

    }


    /**
     * @return **uploadedSome**
     */
    private suspend fun uploadAllSyncErrorsPendingUpload(): Boolean {
        logInfo("uploadSyncErrors")
        var uploadedSome = false
        while (true) {
            val syncErrors = findAllSyncErrorsPendingUploadUseCase.findAllSyncErrorsPendingUpload(BATCH_SIZE_SYNC_ERRORS)
            if (syncErrors.isNotEmpty()) {
                uploadSyncErrorsUseCase.upload(syncErrors)
                uploadedSome = true
            } else {
                logInfo("no more sync errors pending upload")
                break
            }
        }
        return uploadedSome
    }

    /**
     * @return **uploadedSome**
     */
    private suspend fun uploadAllSyncErrorsResolved(): Boolean {
        logInfo("uploadSyncErrorsResolved")
        var uploadedSome = false
        while (true) {
            val syncErrorKeys = findAllSyncErrorKeysResolvedUseCase.findAllSyncErrorKeysResolved(BATCH_SIZE_SYNC_ERROR_KEYS)
            if (syncErrorKeys.isNotEmpty()) {
                uploadResolvedSyncErrorKeysUseCase.uploadResolved(syncErrorKeys)
                uploadedSome = true
            } else {
                logInfo("no more resolved sync errors to upload")
                break
            }
        }
        return uploadedSome
    }

}