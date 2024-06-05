package com.jnj.vaccinetracker.common.data.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.jnj.vaccinetracker.common.data.database.entities.BirthDateEntity
import com.jnj.vaccinetracker.common.data.database.entities.GenderEntity
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantEntity
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantEntityBase
import com.jnj.vaccinetracker.common.data.database.entities.base.ParticipantSyncBase
import com.jnj.vaccinetracker.common.data.database.models.common.RoomAddressModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

data class RoomParticipantModel(
    override val dateModified: DateEntity,
    override val participantUuid: String,
    override val phone: String?,
    override val participantId: String,
    override val nin: String?,
    override val gender: GenderEntity,
    override val birthDate: BirthDateEntity,
    override val isBirthDateAnApproximation: Boolean?,
    @Relation(parentColumn = ParticipantEntity.ID, entityColumn = ParticipantEntity.ID)
    val attributes: List<ParticipantAttributeEntity>,
    @Embedded
    val address: RoomAddressModel?,
    override val locationUuid: String?,
) : ParticipantEntityBase, ParticipantSyncBase