package com.jnj.vaccinetracker.common.data.database.models.draft

import androidx.room.Embedded
import androidx.room.Relation
import com.jnj.vaccinetracker.common.data.database.entities.BirthDateEntity
import com.jnj.vaccinetracker.common.data.database.entities.GenderEntity
import com.jnj.vaccinetracker.common.data.database.entities.base.DraftParticipantEntityBase
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantEntity
import com.jnj.vaccinetracker.common.data.database.models.common.RoomAddressModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState

data class RoomDraftParticipantModel(
    override val participantUuid: String,
    override val phone: String?,
    override val participantId: String,
    override val nin: String?,
    override val gender: GenderEntity,
    override val birthDate: BirthDateEntity,
    override val isBirthDateEstimated: Boolean? = false,
    @Relation(parentColumn = DraftParticipantEntity.ID, entityColumn = DraftParticipantEntity.ID)
    val attributes: List<DraftParticipantAttributeEntity>,
    @Embedded
    val address: RoomAddressModel?,
    override val draftState: DraftState,
    override val registrationDate: DateEntity,
    override val locationUuid: String?,
) : DraftParticipantEntityBase