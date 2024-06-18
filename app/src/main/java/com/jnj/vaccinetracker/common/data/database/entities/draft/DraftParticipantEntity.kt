package com.jnj.vaccinetracker.common.data.database.entities.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.entities.BirthDateEntity
import com.jnj.vaccinetracker.common.data.database.entities.GenderEntity
import com.jnj.vaccinetracker.common.data.database.entities.base.*
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState

@Entity(tableName = "draft_participant",
    indices = [Index(ParticipantEntityBase.COL_PARTICIPANT_ID, unique = true)])
data class DraftParticipantEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(index = true)
    override val phone: String?,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    override val participantId: String,
    override val nin: String?,
    override val gender: GenderEntity,
    override val birthDate: BirthDateEntity,
    override val birthWeight: String?,
    @ColumnInfo(index = true)
    override val isBirthDateEstimated: Boolean?,
    override val registrationDate: DateEntity,
    @ColumnInfo(index = true)
    override val draftState: DraftState = DraftState.initialState(),
    @ColumnInfo(index = true)
    override val locationUuid: String?,

) : DraftParticipantEntityBase {
    companion object {
        const val ID = ParticipantUuidContainer.COL_PARTICIPANT_UUID
    }
}


@Entity(tableName = "draft_participant_attribute", foreignKeys = [
    ForeignKey(
        entity = DraftParticipantEntity::class,
        parentColumns = arrayOf(DraftParticipantEntity.ID),
        childColumns = arrayOf(DraftParticipantEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class DraftParticipantAttributeEntity(
    @Embedded
    @PrimaryKey
    val id: Id,
    override val value: String,
) : ParticipantUuidContainer, AttributeEntityBase {
    data class Id(override val participantUuid: String, override val type: String) : ParticipantUuidContainer, AttributeBase

    override val type: String
        get() = id.type
    override val participantUuid: String
        get() = id.participantUuid

    companion object {
        fun Map.Entry<String, String>.toDraftParticipantAttributeEntity(participantUuid: String) = DraftParticipantAttributeEntity(id = Id(participantUuid, key), value)
    }
}


@Entity(tableName = "draft_participant_address", foreignKeys = [
    ForeignKey(
        entity = DraftParticipantEntity::class,
        parentColumns = arrayOf(DraftParticipantEntity.ID),
        childColumns = arrayOf(DraftParticipantEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class DraftParticipantAddressEntity(
    @PrimaryKey
    override val participantUuid: String,
    override val address1: String?,
    override val address2: String?,
    override val cityVillage: String?,
    override val stateProvince: String?,
    override val country: String?,
    override val countyDistrict: String?,
    override val postalCode: String?,
) : ParticipantUuidContainer, AddressEntityBase