package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.data.models.api.response.ObservationDto
import com.jnj.vaccinetracker.common.data.models.api.response.toMap
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.sync.data.models.SyncRecordBase.Companion.TYPE_DELETE
import com.jnj.vaccinetracker.sync.data.models.SyncRecordBase.Companion.TYPE_UPDATE
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import java.util.*

typealias VisitSyncResponse = SyncResponse<VisitSyncRecord>

@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class VisitSyncRecord : SyncRecordBase {
    abstract val visitUuid: String

    @TypeLabel(TYPE_UPDATE)
    @JsonClass(generateAdapter = true)
    data class Update(
        override val participantUuid: String,
        override val dateModified: SyncDate,
        override val visitUuid: String,
        val visitType: String,
        /**
         * example: "2021-02-11 10:49:26.0"
         */
        val startDatetime: Date,
        val attributes: List<AttributeDto>,
        val observations: List<ObservationDto>,
    ) : VisitSyncRecord() {
        companion object {
            fun Update.toDomain() = Visit(
                startDatetime = startDatetime,
                participantUuid = participantUuid,
                visitUuid = visitUuid,
                attributes = attributes.toMap(),
                observations = observations.toMap(),
                dateModified = dateModified.date,
                visitType = visitType,
            )
        }
    }

    @TypeLabel(TYPE_DELETE)
    @JsonClass(generateAdapter = true)
    data class Delete(
        override val participantUuid: String,
        override val dateModified: SyncDate,
        override val visitUuid: String,
    ) : VisitSyncRecord() {
        companion object {
            fun Delete.toDomain() = DeletedSyncRecord.Visit(visitUuid, participantUuid, dateModified)

        }
    }
}


fun VisitSyncRecord.toFailedSyncRecordDownload(dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
    return FailedSyncRecordDownload.Visit(visitUuid, participantUuid, dateModified, dateLastDownloadAttempt)
}