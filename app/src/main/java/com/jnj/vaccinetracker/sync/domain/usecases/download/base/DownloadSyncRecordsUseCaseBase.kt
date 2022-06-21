package com.jnj.vaccinetracker.sync.domain.usecases.download.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.exceptions.FailedToDownloadAnySyncRecordsInPageException
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.exceptions.SyncResponseValidationException
import com.jnj.vaccinetracker.common.exceptions.TotalSyncScopeRecordCountMismatchException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.data.models.*
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import kotlinx.coroutines.yield

interface DownloadSyncRecordsUseCase {
    suspend fun download(syncRequest: SyncRequest): SyncStatus
}

data class SyncResponseState(val syncStatus: SyncStatus, val tableCount: Long?)

abstract class DownloadSyncRecordsUseCaseBase<T : SyncRecordBase> : DownloadSyncRecordsUseCase {
    protected abstract val syncLogger: SyncLogger
    protected abstract val syncEntityType: SyncEntityType
    protected abstract val validateSyncResponseUseCase: ValidateSyncResponseUseCase
    protected abstract val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase
    protected abstract val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase

    private suspend fun storeSyncRecords(records: List<T>) {
        logInfo("storeSyncRecords: ${records.size}")
        require(records.isNotEmpty())
        var recordDownloadCount = 0
        records.forEach { record ->
            val syncErrorMetadata = SyncErrorMetadata.StoreSyncRecord(
                syncEntityType = syncEntityType,
                participantUuid = record.participantUuid,
                visitUuid = record.visitUuid
            )
            try {
                storeSyncRecord(record)
            } catch (e: Throwable) {
                yield()
                e.rethrowIfFatal()
                logError("error storing syncRecord", e)
                syncLogger.logSyncError(syncErrorMetadata, e)
                storeFailedSyncRecord(record)
                return@forEach
            }
            syncLogger.clearSyncError(syncErrorMetadata)
            deleteFailedSyncRecordUseCase.delete(record.toFailedSyncRecordDownload())
            recordDownloadCount++
        }
        if (recordDownloadCount == 0) {
            // this will cause sync to be skipped
            throw FailedToDownloadAnySyncRecordsInPageException()
        }
    }

    protected abstract fun mapRecordToFailedSyncRecordDownload(record: T, dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload

    private fun T.toFailedSyncRecordDownload(dateLastDownloadAttempt: DateEntity = DateEntity()): FailedSyncRecordDownload {
        return mapRecordToFailedSyncRecordDownload(this, dateLastDownloadAttempt)
    }

    private suspend fun storeFailedSyncRecord(record: T) {
        storeFailedSyncRecordDownloadUseCase.store(record.toFailedSyncRecordDownload())
    }


    protected abstract suspend fun storeSyncRecord(record: T)

    protected abstract suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<T>

    private fun syncErrorMetadata(syncRequest: SyncRequest) = SyncErrorMetadata.GetAllSyncRecordsCall(
        syncEntityType = syncEntityType,
        syncRequest = syncRequest,
    )

    private fun syncValidationError(syncRequest: SyncRequest, syncStatus: SyncStatus) = SyncErrorMetadata.GetAllSyncRecordsCallValidation(syncEntityType, syncRequest, syncStatus)

    private suspend fun fetchRemoteSyncRecordsOrThrow(syncRequest: SyncRequest): SyncResponse<T> {
        return try {
            fetchRemoteSyncRecords(syncRequest).also {
                syncLogger.clearSyncError(syncErrorMetadata(syncRequest))
                validateSyncResponseUseCase.validate(it, syncRequest, syncEntityType)
                syncLogger.clearSyncError(syncValidationError(syncRequest, it.syncStatus))
            }
        } catch (ex: SyncResponseValidationException) {
            logError("sync response validation error $syncEntityType", ex)
            syncLogger.logSyncError(syncValidationError(syncRequest, ex.syncStatus), ex)
            throw ex
        } catch (ex: TotalSyncScopeRecordCountMismatchException) {
            throw ex
        } catch (ex: NoNetworkException) {
            throw ex
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logError("error during get all sync records call", ex)
            syncLogger.logSyncError(syncErrorMetadata(syncRequest), ex)
            throw ex
        }
    }

    private suspend fun download(syncRequest: SyncRequest, lastResponseState: SyncResponseState?): SyncStatus {
        logInfo("download: $syncRequest")
        try {
            val response = fetchRemoteSyncRecordsOrThrow(syncRequest)
            val syncStatus = response.syncStatus
            when (syncStatus) {
                SyncStatus.OUT_OF_SYNC -> storeSyncRecords(response.records)
                SyncStatus.OK -> {
                    logInfo("sync status OK")
                }
            }
            return syncStatus
        } catch (ex: TotalSyncScopeRecordCountMismatchException) {
            val currentResponseState = SyncResponseState(SyncStatus.OK, ex.backendTableCount)
            logError("total record sync call mismatch", ex)
            return if (currentResponseState == lastResponseState) {
                syncLogger.logSyncError(syncValidationError(syncRequest, SyncStatus.OK), ex)
                throw ex
            } else
                download(syncRequest, currentResponseState)
        }
    }

    override suspend fun download(syncRequest: SyncRequest): SyncStatus {
        return download(syncRequest, null)
    }
}