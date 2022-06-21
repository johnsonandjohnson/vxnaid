package com.jnj.vaccinetracker.common.data.database.models.draft.update

import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.jnj.vaccinetracker.common.domain.entities.DraftState

data class RoomUpdateParticipantDraftStateModel(override val participantUuid: String, override val draftState: DraftState) : UploadableDraft, ParticipantUuidContainer