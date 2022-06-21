package com.jnj.vaccinetracker.common.data.database.entities.draft

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.entities.base.ImageBase
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.jnj.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.jnj.vaccinetracker.common.domain.entities.DraftState

@Entity(tableName = "draft_participant_image")
data class DraftParticipantImageEntity(
    @PrimaryKey
    override val participantUuid: String,
    override val imageFileName: String,
    override val draftState: DraftState,
) : ParticipantUuidContainer, ImageBase, UploadableDraft