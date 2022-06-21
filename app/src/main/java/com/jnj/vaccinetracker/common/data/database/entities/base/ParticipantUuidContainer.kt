package com.jnj.vaccinetracker.common.data.database.entities.base

/**
 * is not a base class of a visit but merely a container of data linking to a certain participant.
 * Main purpose is to make sure naming is consistent across the database
 */
interface ParticipantUuidContainer {
    val participantUuid: String

    companion object {
        const val COL_PARTICIPANT_UUID = "participantUuid"
    }
}