package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class SyncStatus {
    OUT_OF_SYNC, OK
}

/**
 * for each sync endpoint, Each record will contain at least these fields
 */
interface SyncRecordBase {
    val participantUuid: String
    val dateModified: SyncDate

    companion object {
        const val TYPE_UPDATE = "update"
        const val TYPE_DELETE = "delete"
    }
}

val SyncRecordBase.uuid: String get() = visitUuid ?: participantUuid

val SyncRecordBase.visitUuid: String?
    get() = when (this) {
        is VisitSyncRecord -> visitUuid
        else -> null
    }

/**
 * Each response will contain the filters that were requested as verification
 */
@JsonClass(generateAdapter = true)
data class SyncResponse<T : SyncRecordBase>(
    val dateModifiedOffset: SyncDate?,
    val syncScope: SyncScopeDto,
    val uuidsWithDateModifiedOffset: List<String>,
    val limit: Int,
    /**
     * serves are extra verification backend is working as intended
     * @returns if there are no records available with specified filters then return [SyncStatus.OK], otherwise [SyncStatus.OUT_OF_SYNC]
     */
    val syncStatus: SyncStatus,
    /**
     * total amount of records in the sync scope => total sync records + total uploaded drafts
     */
    @Json(name = "tableCount")
    val totalSyncScopeRecordCount: Long? = null,
    /**
     * total amount of uploaded records that are expected to sit in the draft table
     */
    @Json(name = "ignoredCount")
    val totalIgnoredRecordCount: Long? = null,
    @Json(name = "voidedTableCount")
    val totalVoidedRecordCount: Long? = null,
    /**
     * a list of records with type depending on the sync endpoint.
     */
    val records: List<T>,
)

