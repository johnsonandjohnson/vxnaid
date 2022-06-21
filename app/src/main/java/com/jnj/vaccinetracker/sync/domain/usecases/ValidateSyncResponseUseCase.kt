package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.exceptions.SyncResponseValidationException
import com.jnj.vaccinetracker.common.exceptions.TotalSyncScopeRecordCountMismatchException
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.sync.data.models.SyncRequest
import com.jnj.vaccinetracker.sync.data.models.SyncResponse
import com.jnj.vaccinetracker.sync.data.models.SyncStatus
import com.jnj.vaccinetracker.sync.domain.usecases.delete.GetDeletedSyncRecordCountUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.GetFailedSyncRecordDownloadCountUseCase
import javax.inject.Inject

class ValidateSyncResponseUseCase @Inject constructor(
    private val getUploadedDraftCountUseCase: GetUploadedDraftCountUseCase,
    private val getSyncRecordCountUseCase: GetSyncRecordCountUseCase,
    private val getFailedSyncRecordDownloadCountUseCase: GetFailedSyncRecordDownloadCountUseCase,
    private val getDeletedSyncRecordCountUseCase: GetDeletedSyncRecordCountUseCase,
) {

    private fun <T> T?.shouldBe(other: T?, name: String) {
        require(this == other) { "Invalid sync response [$name], got '$this' but expected '$other'" }
    }

    private suspend fun validateCounts(optimize: Boolean, totalSyncScopeRecordCount: Long, ignoredCount: Long?, voidedCount: Long?, syncEntityType: SyncEntityType) {
        val failedCount = getFailedSyncRecordDownloadCountUseCase.count(syncEntityType)
        val successCount = getSyncRecordCountUseCase.getCount(syncEntityType)
        val deletedCount = getDeletedSyncRecordCountUseCase.count(syncEntityType)
        val syncEntityCount = successCount + failedCount + deletedCount
        if (optimize) {
            val draftCount = getUploadedDraftCountUseCase.getCount(syncEntityType)
            val count = draftCount + syncEntityCount
            if (count != totalSyncScopeRecordCount) {
                throw TotalSyncScopeRecordCountMismatchException(message = """local sync record count is $syncEntityCount including $draftCount uploaded drafts, $deletedCount voided, $failedCount failed.
                    |But backend totalSyncScopeRecordCount is $totalSyncScopeRecordCount 
                    |of which $ignoredCount are expected to be uploaded drafts
                    |and $voidedCount are expected to be voided""".trimMargin(), totalSyncScopeRecordCount)
            }
        } else if (syncEntityCount != totalSyncScopeRecordCount) {
            throw TotalSyncScopeRecordCountMismatchException(message = """local sync record count is $syncEntityCount including $deletedCount voided, $failedCount failed
                |but backend totalSyncScopeRecordCount is $totalSyncScopeRecordCount
                |of which $voidedCount are expected to be voided""".trimMargin(), totalSyncScopeRecordCount)
        }
    }


    suspend fun validate(syncResponse: SyncResponse<*>, syncRequest: SyncRequest, syncEntityType: SyncEntityType) {
        with(syncResponse) {
            try {
                dateModifiedOffset.shouldBe(syncRequest.dateModifiedOffset, "dateModifiedOffset")
                syncScope.shouldBe(syncRequest.syncScope, "syncScope")
                val elementsMissing = uuidsWithDateModifiedOffset - syncRequest.uuidsWithDateModifiedOffset
                require(elementsMissing.isEmpty()) { "uuidsWithDateModifiedOffset is missing uuids $elementsMissing" }
                require(uuidsWithDateModifiedOffset.size == syncRequest.uuidsWithDateModifiedOffset.size) {
                    "syncRequest.uuidsWithDateModifiedOffset.size " +
                            "${syncRequest.uuidsWithDateModifiedOffset.size} must be equal to syncRequest.uuidsWithDateModifiedOffset.size ${syncRequest.uuidsWithDateModifiedOffset.size}"
                }
                limit.shouldBe(syncRequest.limit, "limit")
                val maxLimit = limit + uuidsWithDateModifiedOffset.size
                require(records.size <= maxLimit) { "record count (${records.size}) must not be greater than limit $limit + uuidsWithDateModifiedOffset $uuidsWithDateModifiedOffset = $maxLimit" }
                if (records.size > limit) {
                    logWarn("records size ${records.size} is greater than limit $limit {}", syncRequest)
                }
                when (syncStatus) {
                    SyncStatus.OUT_OF_SYNC -> require(records.isNotEmpty()) { "got OUT_OF_SYNC status but empty records" }
                    SyncStatus.OK -> require(records.isEmpty()) { "got OK status but not empty records" }
                }.let {}
            } catch (ex: Exception) {
                throw SyncResponseValidationException(ex, syncResponse.syncStatus)
            }
            if (totalSyncScopeRecordCount != null && syncStatus == SyncStatus.OK) {
                validateCounts(optimize = syncRequest.optimize,
                    totalSyncScopeRecordCount = totalSyncScopeRecordCount,
                    ignoredCount = totalIgnoredRecordCount, voidedCount = totalVoidedRecordCount, syncEntityType)
            }
        }

    }
}