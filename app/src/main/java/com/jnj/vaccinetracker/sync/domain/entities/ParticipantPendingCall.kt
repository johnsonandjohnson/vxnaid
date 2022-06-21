package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.common.domain.entities.DraftParticipant
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter

sealed class ParticipantPendingCall : Comparable<ParticipantPendingCall> {
    abstract val participantUuid: String
    abstract val visitUuid: String?
    abstract val locationUuid: String?

    enum class Type {
        REGISTER_PARTICIPANT, CREATE_VISIT, UPDATE_VISIT;
    }

    val type
        get() = when (this) {
            is CreateVisit -> Type.CREATE_VISIT
            is RegisterParticipant -> Type.REGISTER_PARTICIPANT
            is UpdateVisit -> Type.UPDATE_VISIT
        }

    data class CreateVisit(val draftVisit: DraftVisit) : ParticipantPendingCall() {
        override val participantUuid: String
            get() = draftVisit.participantUuid

        override val visitUuid: String
            get() = draftVisit.visitUuid
        override val locationUuid: String
            get() = draftVisit.locationUuid
    }

    data class UpdateVisit(val draftEncounter: DraftVisitEncounter) : ParticipantPendingCall() {
        override val participantUuid: String
            get() = draftEncounter.participantUuid

        override val visitUuid: String
            get() = draftEncounter.visitUuid
        override val locationUuid: String
            get() = draftEncounter.locationUuid
    }

    data class RegisterParticipant(val draftParticipant: DraftParticipant) : ParticipantPendingCall() {
        override val participantUuid: String
            get() = draftParticipant.participantUuid

        override val visitUuid: String?
            get() = null

        override val locationUuid: String?
            get() = draftParticipant.locationUuid

        val participantId get() = draftParticipant.participantId
    }

    override fun compareTo(other: ParticipantPendingCall): Int {
        return type.compareTo(other.type)
    }
}