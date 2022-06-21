package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

typealias ParticipantImageSyncResponse = SyncResponse<ParticipantImageSyncRecord>

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ParticipantImageSyncRecord : SyncRecordBase {

    @TypeLabel(SyncRecordBase.TYPE_UPDATE)
    @JsonClass(generateAdapter = true)
    data class Update(
        override val participantUuid: String,
        override val dateModified: SyncDate,
        /**
         * jpg in base64 format
         */
        val image: String,
    ) : ParticipantImageSyncRecord()

    @TypeLabel(SyncRecordBase.TYPE_DELETE)
    @JsonClass(generateAdapter = true)
    data class Delete(
        override val participantUuid: String,
        override val dateModified: SyncDate,
    ) : ParticipantImageSyncRecord() {
        companion object {
            fun Delete.toDomain() = DeletedSyncRecord.Image(participantUuid, dateModified)
        }
    }
}

fun ParticipantImageSyncRecord.toFailedSyncRecordDownload(dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
    return FailedSyncRecordDownload.Image(participantUuid, dateModified, dateLastDownloadAttempt)
}