package com.jnj.vaccinetracker.common.data.database.entities.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.entities.base.*
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState


@Entity(tableName = "draft_visit_encounter")
data class DraftVisitEncounterEntity(
    @PrimaryKey
    override val visitUuid: String,
    override val startDatetime: DateEntity,
    override val locationUuid: String,
    @ColumnInfo(index = true)
    override val draftState: DraftState = DraftState.initialState(),
    @ColumnInfo(index = true)
    override val participantUuid: String,
) : DraftVisitEncounterEntityBase {
    companion object {
        const val ID = VisitBase.COL_VISIT_UUID
    }
}

@Entity(tableName = "draft_visit_encounter_attribute", foreignKeys = [
    ForeignKey(
        entity = DraftVisitEncounterEntity::class,
        parentColumns = arrayOf(DraftVisitEncounterEntity.ID),
        childColumns = arrayOf(DraftVisitEncounterEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class DraftVisitEncounterAttributeEntity(
    @Embedded
    @PrimaryKey
    val id: Id,
    override val value: String,
) : AttributeEntityBase, VisitEncounterBase {
    data class Id(override val visitUuid: String, override val type: String) : VisitEncounterBase, AttributeBase

    override val type: String
        get() = id.type
    override val visitUuid: String
        get() = id.visitUuid

    companion object {
        fun Map.Entry<String, String>.toDraftVisitEncounterAttributeEntity(visitUuid: String) = DraftVisitEncounterAttributeEntity(id = Id(
            visitUuid,
            key), value)
    }
}

@Entity(tableName = "draft_visit_encounter_observation", foreignKeys = [
    ForeignKey(
        entity = DraftVisitEncounterEntity::class,
        parentColumns = arrayOf(DraftVisitEncounterEntity.ID),
        childColumns = arrayOf(DraftVisitEncounterEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class DraftVisitEncounterObservationEntity(
    @Embedded
    @PrimaryKey
    val id: Id,
    override val value: String,
) : DraftVisitEncounterObservationEntityBase, VisitBase {
    data class Id(override val visitUuid: String, override val name: String) : VisitEncounterBase, DraftVisitEncounterObservationBase

    override val name: String
        get() = id.name
    override val visitUuid: String
        get() = id.visitUuid

    companion object {
        fun Map.Entry<String, String>.toDraftVisitEncounterObservationEntity(visitUuid: String) =
            DraftVisitEncounterObservationEntity(id = Id(visitUuid, key), value)
    }
}