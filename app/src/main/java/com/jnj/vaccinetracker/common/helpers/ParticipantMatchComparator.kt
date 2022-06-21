package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.domain.entities.ParticipantIdentificationCriteria
import com.jnj.vaccinetracker.common.domain.entities.ParticipantMatch

class ParticipantMatchComparator(private val criteria: ParticipantIdentificationCriteria) : Comparator<ParticipantMatch> {

    private class Rank(val phoneMatches: Boolean, val participantIdMatches: Boolean, val matchingScore: Int) : Comparable<Rank> {
        override fun compareTo(other: Rank): Int {
            var compare = other.participantIdMatches
                .compareTo(participantIdMatches)
            if (compare == 0) {
                if (participantIdMatches)
                    compare = other.matchingScore.compareTo(matchingScore)
                if (compare == 0)
                    compare = other.phoneMatches.compareTo(phoneMatches)
                if (compare == 0)
                    compare = other.matchingScore.compareTo(matchingScore)
            }
            return compare
        }

    }

    private fun ParticipantMatch.buildRank() =
        Rank(participantIdMatches = isParticipantIdMatch(criteria.participantId),
            phoneMatches = isPhoneMatch(criteria.phone),
            matchingScore = matchingScore ?: -1
        )

    override fun compare(o1: ParticipantMatch, o2: ParticipantMatch): Int {
        return o1.buildRank().compareTo(o2.buildRank())
    }
}