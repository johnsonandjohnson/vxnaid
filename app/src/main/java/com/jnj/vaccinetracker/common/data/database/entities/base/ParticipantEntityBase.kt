package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.entities.BirthDateEntity
import com.jnj.vaccinetracker.common.data.database.entities.GenderEntity

interface ParticipantEntityBase : ParticipantUuidContainer {
    val phone: String?
    val participantId: String
    val nin: String?
    val gender: GenderEntity
    val birthDate: BirthDateEntity
    // todo birth weight
    val isBirthDateEstimated: Boolean?
    val locationUuid: String?
    val birthWeight: String?

    companion object {
        const val COL_PHONE = "phone"
        const val COL_PARTICIPANT_ID = "participantId"
    }
}