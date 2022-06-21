package com.jnj.vaccinetracker.common.data.database.entities.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.entities.base.*
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState


@Entity(tableName = "draft_visit")
data class DraftVisitEntity(
    @PrimaryKey
    override val visitUuid: String,
    @ColumnInfo(index = true)
    override val participantUuid: String,
    override val visitType: String,
    override val startDatetime: DateEntity,
    override val locationUuid: String,
    @ColumnInfo(index = true)
    override val draftState: DraftState = DraftState.initialState(),
) : DraftVisitEntityBase, UploadableDraft, ParticipantUuidContainer {
    companion object {
        const val ID = VisitBase.COL_VISIT_UUID
    }
}

@Entity(tableName = "draft_visit_attribute", foreignKeys = [
    ForeignKey(
        entity = DraftVisitEntity::class,
        parentColumns = arrayOf(DraftVisitEntity.ID),
        childColumns = arrayOf(DraftVisitEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class DraftVisitAttributeEntity(
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
        fun Map.Entry<String, String>.toDraftVisitAttributeEntity(visitUuid: String) = DraftVisitAttributeEntity(id = Id(visitUuid, key), value)
    }
}