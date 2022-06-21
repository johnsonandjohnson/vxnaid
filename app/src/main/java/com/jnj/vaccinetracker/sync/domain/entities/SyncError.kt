package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.buildStackTraceString
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.models.SyncRequest
import com.jnj.vaccinetracker.sync.data.models.SyncStatus
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

interface SyncErrorBase {
    val metadata: SyncErrorMetadata
    val stackTrace: String
    val key: String
    val dateCreated: DateEntity
}

data class DraftSyncError(
    val metadata: SyncErrorMetadata,
    val stackTrace: Throwable,
)


data class SyncError(
    override val metadata: SyncErrorMetadata,
    override val stackTrace: String,
    override val dateCreated: DateEntity,
    val syncErrorState: SyncErrorState = SyncErrorState.initialState(),
) : SyncErrorBase {
    override val key: String get() = metadata.key
}

fun DraftSyncError.toPersistence(dateCreated: DateEntity = dateNow()) = SyncError(metadata, stackTrace.buildStackTraceString(), dateCreated)

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class SyncErrorMetadata {

    protected fun buildKey(vararg segments: String?): String {
        return "${type}:${segments.dropLastWhile { it == null }.joinToString(",")}"
    }

    @TypeLabel(FindAllRelatedDraftDataPendingUpload.TYPE)
    @JsonClass(generateAdapter = true)
    data class FindAllRelatedDraftDataPendingUpload(val participantUuid: String) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String
            get() = buildKey(participantUuid)

        companion object {
            const val TYPE = "findAllRelatedDraftDataPendingUpload"
        }
    }

    @TypeLabel(ReportSyncCompletedDateCall.TYPE)
    @JsonClass(generateAdapter = true)
    data class ReportSyncCompletedDateCall(val syncCompletedDate: SyncDate) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String
            get() = buildKey()

        companion object {
            const val TYPE = "reportSyncCompletedDateCall"
        }
    }

    @TypeLabel(GetSyncRecordsByUuidsCall.TYPE)
    @JsonClass(generateAdapter = true)
    data class GetSyncRecordsByUuidsCall(
        val syncEntityType: SyncEntityType,
    ) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String
            get() = buildKey(syncEntityType.name)

        companion object {
            const val TYPE = "getAllSyncRecordsByUuidsCall"
        }
    }

    @TypeLabel(GetAllSyncRecordsCall.TYPE)
    @JsonClass(generateAdapter = true)
    data class GetAllSyncRecordsCall(
        val syncEntityType: SyncEntityType,
        val syncRequest: SyncRequest,
    ) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String
            get() = buildKey(syncEntityType.name)

        companion object {
            const val TYPE = "getAllSyncRecordsCall"
        }
    }

    @TypeLabel(GetAllSyncRecordsCallValidation.TYPE)
    @JsonClass(generateAdapter = true)
    data class GetAllSyncRecordsCallValidation(
        val syncEntityType: SyncEntityType,
        val syncRequest: SyncRequest,
        val syncStatus: SyncStatus,
    ) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String
            get() = buildKey(syncEntityType.name)

        companion object {
            const val TYPE = "GetAllSyncRecordsCallValidation"
        }
    }

    @TypeLabel(StoreSyncRecord.TYPE)
    @JsonClass(generateAdapter = true)
    data class StoreSyncRecord(
        val syncEntityType: SyncEntityType,
        val participantUuid: String,
        val visitUuid: String?,
    ) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String
            get() = buildKey(syncEntityType.name, participantUuid, visitUuid)

        companion object {
            const val TYPE = "storeSyncRecord"
        }
    }

    @TypeLabel(UploadBiometricsTemplate.TYPE)
    @JsonClass(generateAdapter = true)
    data class UploadBiometricsTemplate(val participantUuid: String) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String = buildKey(participantUuid)

        companion object {
            const val TYPE = "UploadBiometricsTemplate"
        }
    }

    @TypeLabel(UploadParticipantPendingCall.TYPE)
    @JsonClass(generateAdapter = true)
    data class UploadParticipantPendingCall(
        val pendingCallType: ParticipantPendingCall.Type,
        val participantUuid: String,
        val visitUuid: String?,
        val locationUuid: String?,
        val participantId: String?,
    ) : SyncErrorMetadata() {

        override val type: String
            get() = TYPE
        override val key: String = buildKey(pendingCallType.name, participantUuid, visitUuid)

        companion object {
            const val TYPE = "uploadParticipantPendingCall"
        }
    }

    @TypeLabel(License.TYPE)
    @JsonClass(generateAdapter = true)
    data class License(val licenseType: LicenseType, val action: Action) : SyncErrorMetadata() {
        enum class Action {
            GET_LICENSE_CALL, RELEASE_LICENSE_CALL, ACTIVATE_LICENSE, DEACTIVATE_LICENSE, OBTAIN_ACTIVATED_LICENSE
        }

        override val type: String
            get() = TYPE
        override val key: String = buildKey(licenseType.name, action.name)

        companion object {
            const val TYPE = "license"
        }
    }

    @TypeLabel(MasterDataUpdatesCall.TYPE)
    @JsonClass(generateAdapter = true)
    class MasterDataUpdatesCall : SyncErrorMetadata() {

        override val type: String
            get() = TYPE
        override val key: String = buildKey()

        companion object {
            const val TYPE = "masterDataUpdatesCall"
        }
    }

    @TypeLabel(MasterData.TYPE)
    @JsonClass(generateAdapter = true)
    data class MasterData(val masterDataFile: MasterDataFile, val action: Action) : SyncErrorMetadata() {
        enum class Action {
            /**
             * trouble fetching remote master data including conversion from JSON to DTO
             */
            GET_MASTER_DATA_CALL,

            /**
             * trouble reading or converting stored master data JSON to DTO
             */
            READ_PERSISTED_MASTER_DATA,

            /**
             * trouble saving master data JSON to disk
             */
            PERSIST_MASTER_DATA,

            /**
             * trouble mapping master data to domain (business logic)
             */
            MAP_MASTER_DATA
        }

        override val type: String
            get() = TYPE

        override val key: String = buildKey(masterDataFile.name, action.name)

        companion object {
            const val TYPE = "masterData"
        }
    }

    @TypeLabel(DeviceNameCall.TYPE)
    @JsonClass(generateAdapter = true)
    data class DeviceNameCall(val siteUuid: String) : SyncErrorMetadata() {
        override val type: String
            get() = TYPE
        override val key: String = buildKey()

        companion object {
            const val TYPE = "deviceNameCall"
        }
    }

    abstract val key: String
    abstract val type: String
}