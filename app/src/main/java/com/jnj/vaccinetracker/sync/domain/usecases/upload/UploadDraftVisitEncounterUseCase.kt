package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.models.api.request.UpdateVisitObservationDto
import com.jnj.vaccinetracker.common.data.models.api.request.VisitUpdateRequest
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class UploadDraftVisitEncounterUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
) {

    private fun DraftVisitEncounter.toDto() = VisitUpdateRequest(
        visitUuid = visitUuid,
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        attributes = attributes.map { AttributeDto(it.key, it.value) },
        observations = observations.map { UpdateVisitObservationDto(it.key, it.value) },
    )

    private suspend fun updateDraftStates(uploadedDraftVisitEncounter: DraftVisitEncounter) {
        draftVisitEncounterRepository.updateDraftState(uploadedDraftVisitEncounter)
    }

    suspend fun upload(draftVisitEncounter: DraftVisitEncounter) {
        require(draftVisitEncounter.draftState.isPendingUpload()) { "VisitEncounter already uploaded!" }
        val request = draftVisitEncounter.toDto()
        api.updateVisit(request)
        val uploadedDraftVisitEncounter = draftVisitEncounter.copy(
            draftState = DraftState.UPLOADED)
        updateDraftStates(uploadedDraftVisitEncounter)
    }

}