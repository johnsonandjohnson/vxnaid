package com.jnj.vaccinetracker.common.data.database.models.draft.update

import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraftWithDate
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState

data class RoomUpdateParticipantDraftStateWithDateModel(
    override val participantUuid: String,
    override val draftState: DraftState,
    override val dateLastUploadAttempt: DateEntity?,
) : UploadableDraftWithDate, ParticipantUuidContainer