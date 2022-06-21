package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

typealias ParticipantBiometricsTemplateSyncResponse = SyncResponse<ParticipantBiometricsTemplateSyncRecord>

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ParticipantBiometricsTemplateSyncRecord : SyncRecordBase {
    @TypeLabel(SyncRecordBase.TYPE_UPDATE)
    @JsonClass(generateAdapter = true)
    data class Update(
        override val participantUuid: String,
        override val dateModified: SyncDate,
        /**
         * binary NTemplate in base64 format (iris data for one or two eyes)
         */
        val biometricsTemplate: String,
    ) : ParticipantBiometricsTemplateSyncRecord()

    @TypeLabel(SyncRecordBase.TYPE_DELETE)
    @JsonClass(generateAdapter = true)
    data class Delete(
        override val participantUuid: String,
        override val dateModified: SyncDate,
    ) : ParticipantBiometricsTemplateSyncRecord() {
        companion object {
            fun Delete.toDomain() = DeletedSyncRecord.BiometricsTemplate(participantUuid, dateModified)
        }
    }
}

fun ParticipantBiometricsTemplateSyncRecord.toFailedSyncRecordDownload(dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
    return FailedSyncRecordDownload.BiometricsTemplate(participantUuid, dateModified, dateLastDownloadAttempt)
}