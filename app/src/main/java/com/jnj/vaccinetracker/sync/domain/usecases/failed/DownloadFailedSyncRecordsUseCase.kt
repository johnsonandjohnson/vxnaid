package com.jnj.vaccinetracker.sync.domain.usecases.failed

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.models.*
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantBiometricsTemplateSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantImageSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreVisitSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.base.StoreSyncRecordUseCaseBase
import kotlinx.coroutines.yield
import javax.inject.Inject

class DownloadFailedSyncRecordsUseCase @Inject constructor(
    private val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
    private val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantSyncRecordUseCase: StoreParticipantSyncRecordUseCase,
    private val storeVisitSyncRecordUseCase: StoreVisitSyncRecordUseCase,
    private val storeParticipantImageSyncRecordUseCase: StoreParticipantImageSyncRecordUseCase,
    private val storeParticipantBiometricsTemplateSyncRecordUseCase: StoreParticipantBiometricsTemplateSyncRecordUseCase,
    private val syncLogger: SyncLogger,
) {

    private suspend fun <R : SyncRecordBase> storeSyncRecord(
        syncEntityType: SyncEntityType,
        storeRecordUseCase: StoreSyncRecordUseCaseBase<R, *>,
        record: R,
        failedRecord: FailedSyncRecordDownload,
    ) {
        val syncErrorMetadata = SyncErrorMetadata.StoreSyncRecord(syncEntityType, participantUuid = record.participantUuid, visitUuid = record.visitUuid)
        try {
            storeRecordUseCase.store(record)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("storeSyncRecordFailed call failed", ex)
            syncLogger.logSyncError(syncErrorMetadata, ex)
            storeFailedSyncRecordDownloadUseCase.store(failedRecord.refreshLastDownloadAttemptDate())
            return
        }
        deleteFailedSyncRecordUseCase.delete(failedRecord)
        syncLogger.clearSyncError(syncErrorMetadata)
    }

    private fun validateRecords(failedSyncRecordDownloads: List<FailedSyncRecordDownload>, records: List<SyncRecordBase>) {
        val uuids = failedSyncRecordDownloads.map { it.uuid }
        val uuidsMissing = uuids - records.map { it.uuid }
        require(uuidsMissing.isEmpty()) { "Following uuids missing: $uuidsMissing" }
    }

    private suspend fun <R : SyncRecordBase> downloadSyncRecords(
        syncEntityType: SyncEntityType,
        storeRecordUseCase: StoreSyncRecordUseCaseBase<R, *>,
        failedSyncRecordDownloads: List<FailedSyncRecordDownload>,
        syncRecordsCall: suspend (uuids: List<String>) -> List<R>,
    ) {
        fun findFailedSyncRecord(uuid: String): FailedSyncRecordDownload = failedSyncRecordDownloads.find { it.uuid == uuid }
            ?: error("received record ($uuid, $syncEntityType) that was not requested")

        fun R.uuid() = visitUuid ?: participantUuid
        val syncErrorMetadata = SyncErrorMetadata.GetSyncRecordsByUuidsCall(syncEntityType)
        val uuids = failedSyncRecordDownloads.map { it.uuid }
        val records = try {
            syncRecordsCall(uuids).also { records ->
                validateRecords(failedSyncRecordDownloads, records)
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("getSyncRecordsByUuids call failed", ex)
            syncLogger.logSyncError(syncErrorMetadata, ex)
            return
        }
        //store each record one by one
        records.forEach { record ->
            storeSyncRecord(syncEntityType, storeRecordUseCase, record, findFailedSyncRecord(record.uuid()))
        }
    }

    private suspend fun downloadByUuids(syncEntityType: SyncEntityType, failedSyncRecordDownloads: List<FailedSyncRecordDownload>) {
        when (syncEntityType) {
            SyncEntityType.PARTICIPANT -> downloadSyncRecords(syncEntityType, storeParticipantSyncRecordUseCase,
                failedSyncRecordDownloads
            ) { api.getParticipantsByUuids(GetParticipantsByUuidsRequest(it)) }
            SyncEntityType.IMAGE -> downloadSyncRecords(syncEntityType, storeParticipantImageSyncRecordUseCase,
                failedSyncRecordDownloads
            ) { api.getImagesByUuids(GetImagesByUuidsRequest(it)) }
            SyncEntityType.BIOMETRICS_TEMPLATE -> downloadSyncRecords(syncEntityType, storeParticipantBiometricsTemplateSyncRecordUseCase,
                failedSyncRecordDownloads
            ) { api.getBiometricsTemplatesByUuids(GetBiometricsTemplatesByUuidsRequest(it)) }
            SyncEntityType.VISIT -> downloadSyncRecords(syncEntityType, storeVisitSyncRecordUseCase,
                failedSyncRecordDownloads
            ) { api.getVisitsByUuids(GetVisitsByUuidsRequest(it)) }
        }.let { }
    }

    suspend fun download(failedSyncRecordDownloads: List<FailedSyncRecordDownload>) {
        require(failedSyncRecordDownloads.isNotEmpty())
        logInfo("download: ${failedSyncRecordDownloads.firstOrNull()?.syncEntityType}")
        val syncEntityTypeMap = failedSyncRecordDownloads.groupBy { it.syncEntityType }
        syncEntityTypeMap.forEach { (syncEntityType, records) ->
            downloadByUuids(syncEntityType, records)
        }
    }
}