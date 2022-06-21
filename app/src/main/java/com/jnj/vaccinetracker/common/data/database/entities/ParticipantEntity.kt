package com.jnj.vaccinetracker.common.data.database.entities

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.entities.base.*
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

@Entity(tableName = "participant",
    indices = [Index(ParticipantEntityBase.COL_PARTICIPANT_ID, unique = true)])
data class ParticipantEntity(
    @PrimaryKey
    override val participantUuid: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    override val participantId: String,
    @ColumnInfo(index = true)
    override val phone: String?,
    override val gender: GenderEntity,
    override val birthDate: BirthDateEntity,
    @ColumnInfo(index = true)
    override val dateModified: DateEntity,
    @ColumnInfo(index = true)
    override val locationUuid: String?,
) : ParticipantEntityBase, ParticipantSyncBase {
    companion object {
        const val ID = ParticipantUuidContainer.COL_PARTICIPANT_UUID
    }
}

@Entity(tableName = "participant_attribute", foreignKeys = [
    ForeignKey(
        entity = ParticipantEntity::class,
        parentColumns = arrayOf(ParticipantEntity.ID),
        childColumns = arrayOf(ParticipantEntity.ID),
        onDelete = ForeignKey.CASCADE
    ),
])
data class ParticipantAttributeEntity(
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
        fun Map.Entry<String, String>.toParticipantAttributeEntity(participantUuid: String) = ParticipantAttributeEntity(id = Id(participantUuid, key), value)
    }
}


@Entity(tableName = "participant_address",
    foreignKeys = [
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = arrayOf(ParticipantEntity.ID),
            childColumns = arrayOf(ParticipantEntity.ID),
            onDelete = ForeignKey.CASCADE
        ),
    ])
data class ParticipantAddressEntity(
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