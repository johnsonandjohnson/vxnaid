package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.*
import com.jnj.vaccinetracker.common.helpers.flattenMerge
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDraftChangesUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
) {

    fun observeDraftChanges(): Flow<*> {
        return listOf(
            draftParticipantRepository.observeChanges(),
            draftParticipantImageRepository.observeChanges(),
            draftParticipantBiometricsTemplateRepository.observeChanges(),
            draftVisitRepository.observeChanges(),
            draftVisitEncounterRepository.observeChanges(),
        ).flattenMerge()
    }
}