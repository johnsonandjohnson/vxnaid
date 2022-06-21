package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.entities.base.VisitBase
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

sealed class FailedSyncRecordDownloadEntityBase : SyncBase {
    abstract val uuid: String
    abstract val dateLastDownloadAttempt: DateEntity
}

@Entity(tableName = "failed_participant_download")
data class FailedParticipantSyncRecordDownloadEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateLastDownloadAttempt: DateEntity,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : FailedSyncRecordDownloadEntityBase(), ParticipantUuidContainer {

    override val uuid: String
        get() = participantUuid
}

@Entity(tableName = "failed_image_download")
data class FailedImageSyncRecordDownloadEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateLastDownloadAttempt: DateEntity,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : FailedSyncRecordDownloadEntityBase(), ParticipantUuidContainer {

    override val uuid: String
        get() = participantUuid
}

@Entity(tableName = "failed_biometrics_template_download")
data class FailedBiometricsTemplateSyncRecordDownloadEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateLastDownloadAttempt: DateEntity,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : FailedSyncRecordDownloadEntityBase(), ParticipantUuidContainer {

    override val uuid: String
        get() = participantUuid
}


@Entity(tableName = "failed_visit_download")
data class FailedVisitSyncRecordDownloadEntity(
    @PrimaryKey
    override val visitUuid: String,
    @ColumnInfo(index = true)
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateLastDownloadAttempt: DateEntity,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : FailedSyncRecordDownloadEntityBase(), ParticipantUuidContainer, VisitBase {

    override val uuid: String
        get() = visitUuid
}

