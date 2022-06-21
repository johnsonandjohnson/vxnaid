package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantSyncBase
import com.jnj.vaccinetracker.common.data.database.entities.base.SyncBase
import com.jnj.vaccinetracker.common.data.database.entities.base.VisitSyncBase
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

sealed class DeletedSyncRecordEntityBase : SyncBase {
    abstract val uuid: String
}

@Entity(tableName = "deleted_participant_biometrics_template")
data class DeletedParticipantBiometricsTemplateEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : DeletedSyncRecordEntityBase(), ParticipantSyncBase {

    override val uuid: String
        get() = participantUuid
}


@Entity(tableName = "deleted_participant")
data class DeletedParticipantEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(defaultValue = "")
    val participantId: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : DeletedSyncRecordEntityBase(), ParticipantSyncBase {

    override val uuid: String
        get() = participantUuid
}


@Entity(tableName = "deleted_participant_image")
data class DeletedParticipantImageEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : DeletedSyncRecordEntityBase(), ParticipantSyncBase {

    override val uuid: String
        get() = participantUuid
}


@Entity(tableName = "deleted_visit")
data class DeletedVisitEntity(
    @PrimaryKey
    override val visitUuid: String,
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : DeletedSyncRecordEntityBase(), VisitSyncBase {

    override val uuid: String
        get() = visitUuid
}
