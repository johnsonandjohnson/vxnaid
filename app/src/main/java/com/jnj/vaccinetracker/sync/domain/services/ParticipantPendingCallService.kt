package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.domain.entities.ParticipantPendingCall
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.usecases.FindAllRelatedDraftParticipantDataPendingUploadUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.FindDraftParticipantUuidsPendingUploadUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.ObserveDraftChangesUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.error.FindUnresolvedSyncErrorKeysByTypeUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.error.ObserveSyncErrorsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.upload.UploadDraftParticipantDataUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * must be used in Android foreground service
 */
@Singleton
class ParticipantPendingCallService @Inject constructor(
    private val findDraftParticipantUuidsPendingUploadUseCase: FindDraftParticipantUuidsPendingUploadUseCase,
    private val uploadDraftParticipantDataUseCase: UploadDraftParticipantDataUseCase,
    private val findAllRelatedDraftParticipantDataPendingUploadUseCase: FindAllRelatedDraftParticipantDataPendingUploadUseCase,
    private val networkConnectivity: NetworkConnectivity,
    private val observeDraftChangesUseCase: ObserveDraftChangesUseCase,
    private val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsObserver: SyncSettingsObserver,
    private val observeSyncErrorsUseCase: ObserveSyncErrorsUseCase,
    private val findUnresolvedSyncErrorKeysByTypeUseCase: FindUnresolvedSyncErrorKeysByTypeUseCase,
    private val syncLogger: SyncLogger,
) {
    private val skipParticipantUuidMap = mutableMapOf<String, Boolean>()

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var uploadJob: Job? = null
    private var databaseObserveJob: Job? = null
    private var errorObservableJob: Job? = null

    companion object {
        private val counter = Counters.UpstreamSync
    }

    fun startUploading() {
        startObserveDatabaseJob()
        startObserveErrorsJob()
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeDatabase() {
        observeDraftChangesUseCase.observeDraftChanges()
            .debounce(counter.OBSERVE_DRAFT_TABLES_DELAY)
            .collectLatest { _ ->
                logInfo("observeDraftChanges collect")
                uploadJob?.join()
                startUploadJob()
            }
    }


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private suspend fun observeErrors() {
        observeSyncErrorsUseCase.observeChanges()
            .debounce(counter.OBSERVE_ERRORS_DEBOUNCE)
            .map {
                // find all keys for the upload errors
                findUnresolvedSyncErrorKeysByTypeUseCase.findUnresolvedSyncErrorKeysByType(SyncErrorMetadata.UploadParticipantPendingCall.TYPE)
            }.mapLatest { existingErrorKeys ->
                // create a map of the skipped participant uuid to all possible error keys
                val skipUUidByPossibleKeysMap = skipParticipantUuidMap.keys.map { skipUuid ->
                    skipUuid to ParticipantPendingCall.Type.values()
                        .map { SyncErrorMetadata.UploadParticipantPendingCall(it, participantUuid = skipUuid, visitUuid = null, null, null).key }
                }.toMap()
                // filter the skipped uuids that have related errors in the database by checking the error keys
                skipUUidByPossibleKeysMap.filterValues { possibleKeys ->
                    possibleKeys.none { possibleKey -> existingErrorKeys.none { it.startsWith(possibleKey) } }
                }.map { it.key }
            }.flowOn(dispatchers.computation)
            .collect { skipUuidsToUndo ->
                undoSkipUuids(skipUuidsToUndo)
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

    private fun startObserveDatabaseJob() {
        if (databaseObserveJob?.isActive != true) {
            scope.launch {
                observeDatabase()
            }.also {
                databaseObserveJob = it
            }
        }
    }

    private fun startUploadJob() {
        logInfo("startUploadJob")
        if (uploadJob?.isActive != true) {
            scope.launch {
                upload()
            }.also {
                uploadJob = it
            }
        }
    }

    private suspend fun upload() {
        syncLogger.logUploadInProgress(true)
        logInfo("upload")
        try {
            val uuids = findDraftParticipantUuidsPendingUploadUseCase.findParticipantUuidsPendingUpload(skipParticipantUuidMap)
            if (uuids.isEmpty()) {
                // no participant data to upload that's not skipped
                logInfo("no more uuids found to upload")
                return
            }
            for (participantUuid in uuids) {
                uploadParticipant(participantUuid, counter.UPLOAD_RETRY_COUNT)
            }
            // we call this method again because the database has changed since we last fetched the uuids
            upload()
        } finally {
            syncLogger.logUploadInProgress(false)
        }
    }

    private suspend fun uploadParticipant(participantUuid: String, retriesLeft: Int) {
        require(retriesLeft >= 0) { "retriesLeft ($retriesLeft) must not be negative" }
        logInfo("uploadParticipant $participantUuid $retriesLeft")
        if (retriesLeft == 0) {
            logInfo("no retries left, skipping participantUuid $participantUuid")
            skipUuid(participantUuid)
            return
        }
        networkConnectivity.awaitFastInternet(debugLabel())
        syncSettingsObserver.awaitSyncSettingsAvailable(debugLabel())
        val logSyncErrors = retriesLeft == 1 // only log sync errors after last attempt
        val success = upload(participantUuid, logSyncErrors)
        if (!success) {
            if (networkConnectivity.isConnectedFast() && syncSettingsObserver.isSyncSettingsAvailable()) {
                delay(counter.RETRY_DELAY)
                uploadParticipant(participantUuid, retriesLeft - 1)
            } else {
                //if we are offline, don't decrement retries
                uploadParticipant(participantUuid, retriesLeft)
            }
        }
    }

    private fun undoSkipUuids(participantUuids: List<String>) {
        participantUuids.forEach {
            skipParticipantUuidMap[it] = false
        }
        startUploadJob()
    }

    private fun undoSkipUuid(participantUuid: String) {
        skipParticipantUuidMap[participantUuid] = false
        startUploadJob()
    }

    private fun undoSkipUuidDelayed(participantUuid: String) {
        scope.launch(dispatchers.io) {
            delaySafe(counter.MAX_SKIP_DURATION)
            undoSkipUuid(participantUuid)
        }
    }

    /**
     *   if something goes wrong just skip it in memory for now.
     *   This would mean if the app is restarted we would try the skipped uuids again.
     */
    private fun skipUuid(participantUuid: String, undoLater: Boolean = true) {
        logInfo("skipUuid: $participantUuid")
        skipParticipantUuidMap[participantUuid] = true
        if (undoLater)
            undoSkipUuidDelayed(participantUuid)
    }

    /**
     * @return success
     */
    private suspend fun upload(participantUuid: String, logSyncErrors: Boolean): Boolean {
        logInfo("upload findAllRelatedDraftDataPendingUpload")
        val participantDataModels = try {
            findAllRelatedDraftParticipantDataPendingUploadUseCase.findAllRelatedDraftDataPendingUpload(participantUuid, logSyncErrors)
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            logError("something went wrong fetching data to upload related to participantUuid $participantUuid", throwable)
            return false
        }
        if (participantDataModels.isEmpty()) {
            logWarn("Warning participant with id $participantUuid doesn't have any data to upload")
            return true
        }
        try {
            for (participantDataModel in participantDataModels) {
                uploadDraftParticipantDataUseCase.upload(participantDataModel, logSyncErrors)
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            logError("something went wrong uploading data related to participantUuid $participantUuid", throwable)
            return false
        }

        return true
    }

}