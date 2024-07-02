package com.jnj.vaccinetracker.sync.domain.helpers

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.datasources.SyncCompletedDateDataSource
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.exceptions.StoreSyncErrorException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.DraftSyncError
import com.jnj.vaccinetracker.sync.domain.entities.MasterSyncStatus
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.entities.SyncPageProgress
import com.jnj.vaccinetracker.sync.domain.usecases.error.ClearSyncErrorUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.error.StoreSyncErrorUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLogger @Inject constructor(
    private val storeSyncErrorUseCase: StoreSyncErrorUseCase,
    private val clearSyncErrorUseCase: ClearSyncErrorUseCase,
    private val dispatchers: AppCoroutineDispatchers,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
    private val syncCompletedDateDataSource: SyncCompletedDateDataSource,
) {
    private val masterDataPersistedMap = MasterDataFile.values().map { it to MutableSharedFlow<Unit>(0, 10) }.toMap()
    private val job = SupervisorJob()
    private val syncPageProgressMap = SyncEntityType.values().map { it to MutableStateFlow(SyncPageProgress.IDLE) }.toMap()
    private val isParticipantUploadInProgress = MutableStateFlow(false)
    private val isParticipantDownloadInProgress = MutableStateFlow(false)
    private val isFailedBiometricsTemplateUploadInProgress = MutableStateFlow(false)
    private val isMasterDataSyncInProgress = MutableStateFlow(false)

    /**
     * the log methods are side effects and thus they should not suspend the caller coroutine
     */
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)


    fun logFailedBiometricsTemplateUploadInProgress(inProgress: Boolean) {
        isFailedBiometricsTemplateUploadInProgress.value = inProgress
    }

    fun logUploadInProgress(inProgress: Boolean) {
        isParticipantUploadInProgress.value = inProgress
    }

    /**
     * Note that if we go offline during sync, this method will wait until we go back online and sync resumes
     */
    suspend fun awaitSyncNotInProgress() = isParticipantDownloadInProgress.filter { inProgress -> !inProgress }.await()

    fun observeSyncInProgress() = observeParticipantDownloadInProgress()

    suspend fun awaitFailedBiometricsTemplateUploadNotInProgress() = isFailedBiometricsTemplateUploadInProgress.filter { inProgress -> !inProgress }.await()
    fun observeFailedBiometricsTemplateUploadInProgress(): Flow<Boolean> = isFailedBiometricsTemplateUploadInProgress
    fun observeUploadInProgress(): Flow<Boolean> = isParticipantUploadInProgress

    fun isMasterDataSyncInProgress(): Boolean = isMasterDataSyncInProgress.value

    fun observeMasterDataSyncInProgress(): Flow<Boolean> = isMasterDataSyncInProgress

    fun logMasterDataSyncInProgress(inProgress: Boolean) {
        isMasterDataSyncInProgress.value = inProgress
    }

    fun isParticipantDownloadInProgress(): Boolean = isParticipantDownloadInProgress.value

    fun observeParticipantDownloadInProgress(): Flow<Boolean> = isParticipantDownloadInProgress

    fun logParticipantDownloadInProgress(inProgress: Boolean) {
        isParticipantDownloadInProgress.value = inProgress
    }

    fun isSyncPageProgressNotDownloading(): Boolean = SyncEntityType.values()
        .all { getSyncPageProgress(it).isNotDownloading() }

    /**
     * we need to wait until sync has stopped downloading records.
     */
    suspend fun awaitSyncPageProgressNotDownloading() = SyncEntityType.values().map { observeSyncPageProgress(it) }
        .flattenMerge().filter { isSyncPageProgressNotDownloading() }.await()

    fun observeMasterDataPersisted(masterDataFile: MasterDataFile): Flow<Unit> = masterDataPersistedMap[masterDataFile]!!

    fun observeMasterDataLoadedInMemory(masterDataFile: MasterDataFile): Flow<Unit> {
        return when (masterDataFile) {
            MasterDataFile.CONFIGURATION -> masterDataMemoryDataSource.observeConfiguration().map { }
            MasterDataFile.SITES -> masterDataMemoryDataSource.observeSites().map { }
            MasterDataFile.LOCALIZATION -> masterDataMemoryDataSource.observeLocalization().map { }
            MasterDataFile.ADDRESS_HIERARCHY -> masterDataMemoryDataSource.observeAddressHierarchy().map { }
            MasterDataFile.VACCINE_SCHEDULE -> masterDataMemoryDataSource.observeVaccineSchedule().map { }
            MasterDataFile.SUBSTANCES_CONFIG -> masterDataMemoryDataSource.observeSubstanceConfig().map { }
            MasterDataFile.SUBSTANCES_GROUP_CONFIG -> masterDataMemoryDataSource.observeSubstanceConfig().map { }
        }
    }

    fun logSyncPageProgress(syncEntityType: SyncEntityType, syncPageProgress: SyncPageProgress) {
        this.syncPageProgressMap[syncEntityType]!!.value = syncPageProgress
    }

    fun observeSyncPageProgress(syncEntityType: SyncEntityType): Flow<SyncPageProgress> = syncPageProgressMap[syncEntityType]!!

    fun getSyncPageProgress(syncEntityType: SyncEntityType) = syncPageProgressMap[syncEntityType]!!.value

    fun logMasterDataPersisted(masterDataFile: MasterDataFile) {
        scope.launch {
            masterDataPersistedMap[masterDataFile]!!.emit(Unit)
        }
    }

    fun logSyncCompletedDate(syncDate: SyncDate) {
        logInfo("logSyncCompletedDate: $syncDate")
        syncCompletedDateDataSource.syncParticipantDateCompletedPref.set(syncDate.time)
    }

    fun logMasterSyncStatus(masterDataFile: MasterDataFile, syncStatus: MasterSyncStatus, date: SyncDate) {
        logInfo("logMasterSyncStatus masterData $masterDataFile $syncStatus $date")
    }


    fun isInProgress(): Boolean = isParticipantDownloadInProgress.value

    fun observeSyncCompletedDate(): Flow<SyncDate?> = syncCompletedDateDataSource.syncParticipantDateCompletedPref.asFlow().map { it.toDate() }

    fun getSyncCompletedDate(): SyncDate? = syncCompletedDateDataSource.syncParticipantDateCompletedPref.get().toDate()

    fun clearSyncError(metadata: SyncErrorMetadata) {
        scope.launch {
            logInfo("clearSyncError: {}", metadata)
            try {
                clearSyncErrorUseCase.clearSyncError(metadata)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("clearSyncError failed to clear sync error: $metadata", ex)
            }
        }
    }

    fun logSyncError(metadata: SyncErrorMetadata, stackTrace: Throwable) {
        scope.launch {
            try {
                storeSyncErrorUseCase.store(DraftSyncError(metadata, stackTrace))
            } catch (ex: StoreSyncErrorException) {
                logError("logSyncError store sync record failed", ex)
            }
        }
    }

    fun logSyncCompletedDateReported(syncDate: SyncDate) {
        scope.launch {
            syncCompletedDateDataSource.lastReportedSyncDatePref.set(syncDate.time)
        }
    }

    fun getLastReportedSyncCompletedDate(): SyncDate? = syncCompletedDateDataSource.lastReportedSyncDatePref.get().toDate()

    private fun Long.toDate(): SyncDate? = takeIf { it > 0L }
        ?.let { SyncDate(it) }
}