package com.jnj.vaccinetracker.common.data.database.models.draft

import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer

data class RoomDraftParticipantDataToUploadModel(override val participantUuid: String) : ParticipantUuidContainer