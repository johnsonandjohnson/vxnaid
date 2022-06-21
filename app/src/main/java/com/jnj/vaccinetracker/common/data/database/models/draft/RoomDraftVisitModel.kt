package com.jnj.vaccinetracker.common.data.database.models.draft

import androidx.room.Relation
import com.jnj.vaccinetracker.common.data.database.entities.base.DraftVisitEntityBase
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState

data class RoomDraftVisitModel(
    override val startDatetime: DateEntity,
    override val locationUuid: String,
    override val visitUuid: String,
    @Relation(parentColumn = DraftVisitEntity.ID, entityColumn = DraftVisitEntity.ID)
    val attributes: List<DraftVisitAttributeEntity>,
    override val draftState: DraftState = DraftState.initialState(),
    override val participantUuid: String,
    override val visitType: String,
) : DraftVisitEntityBase, ParticipantUuidContainer