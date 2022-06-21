package com.jnj.vaccinetracker.common.domain.entities

data class LocalParticipantMatch(val participant: ParticipantBase, val matchingScore: Int? = null) {
    val uuid get() = participant.participantUuid
}