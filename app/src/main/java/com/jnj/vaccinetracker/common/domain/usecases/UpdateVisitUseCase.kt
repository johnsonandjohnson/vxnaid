package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.UpdateVisit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateVisitUseCase @Inject constructor(
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
) {
    private fun UpdateVisit.toDomain() = DraftVisitEncounter(
        startDatetime = startDatetime,
        participantUuid = participantUuid,
        locationUuid = locationUuid,
        visitUuid = visitUuid,
        attributes = attributes,
        observations = observations,
        draftState = DraftState.initialState()
    )

    suspend fun updateVisit(updateVisit: UpdateVisit): DraftVisitEncounter {
        val visitEncounter = updateVisit.toDomain()
        draftVisitEncounterRepository.insert(visitEncounter, orReplace = false)
        return visitEncounter
    }
}