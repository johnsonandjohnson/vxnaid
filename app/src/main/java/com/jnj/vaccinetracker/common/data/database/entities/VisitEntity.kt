package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.entities.base.*
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue


@Entity(tableName = "visit")
data class VisitEntity(
    @PrimaryKey
    override val visitUuid: String,
    @ColumnInfo(index = true)
    override val participantUuid: String,
    override val visitType: String,
    override val startDatetime: DateEntity,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
) : VisitEntityBase, VisitSyncBase, ParticipantUuidContainer {
    companion object {
        const val ID = VisitBase.COL_VISIT_UUID
    }
}

@Entity(tableName = "visit_attribute", foreignKeys = [
    ForeignKey(
        entity = VisitEntity::class,
        parentColumns = arrayOf(VisitEntity.ID),
        childColumns = arrayOf(VisitEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class VisitAttributeEntity(
    @Embedded
    @PrimaryKey
    val id: Id,
    override val value: String,
) : AttributeEntityBase, VisitBase {
    data class Id(override val visitUuid: String, override val type: String) : VisitBase, AttributeBase

    override val type: String
        get() = id.type
    override val visitUuid: String
        get() = id.visitUuid

    companion object {
        fun Map.Entry<String, String>.toVisitAttributeEntity(visitUuid: String) = VisitAttributeEntity(id = Id(visitUuid, key), value)
    }
}


@Entity(tableName = "visit_observation", foreignKeys = [
    ForeignKey(
        entity = VisitEntity::class,
        parentColumns = arrayOf(VisitEntity.ID),
        childColumns = arrayOf(VisitEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class VisitObservationEntity(
    @Embedded
    @PrimaryKey
    val id: Id,
    override val value: String,
    override val dateTime: DateEntity,
) : VisitObservationEntityBase, VisitBase {
    data class Id(override val visitUuid: String, override val name: String) : VisitBase, DraftVisitEncounterObservationBase

    override val name: String
        get() = id.name
    override val visitUuid: String
        get() = id.visitUuid


    companion object {
        fun Map.Entry<String, ObservationValue>.toVisitObservationEntity(visitUuid: String) = VisitObservationEntity(id = Id(visitUuid, key), value.value, value.dateTime)
    }
}