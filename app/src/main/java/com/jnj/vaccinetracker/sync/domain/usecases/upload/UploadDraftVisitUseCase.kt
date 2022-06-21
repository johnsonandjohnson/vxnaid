package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.models.api.request.CreateVisitAttributeDto
import com.jnj.vaccinetracker.common.data.models.api.request.VisitCreateRequest
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.exceptions.DuplicateRequestException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class UploadDraftVisitUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val draftVisitRepository: DraftVisitRepository,
) {

    private fun DraftVisit.toDto() = VisitCreateRequest(
        participantUuid = participantUuid,
        visitType = visitType,
        startDatetime = startDatetime,
        visitUuid = visitUuid,
        locationUuid = locationUuid,
        attributes = attributes.map { CreateVisitAttributeDto(it.key, it.value) }
    )

    private suspend fun updateDraftStates(uploadedDraftVisit: DraftVisit) {
        draftVisitRepository.updateDraftState(uploadedDraftVisit)
    }

    suspend fun upload(draftVisit: DraftVisit) {
        require(draftVisit.draftState.isPendingUpload()) { "Visit already uploaded!" }
        val request = draftVisit.toDto()
        try {
            api.createVisit(request)
        } catch (ex: WebCallException) {
            when (ex.cause) {
                is DuplicateRequestException -> {
                    // ignore exception and pretend visit was created so draft state becomes uploaded
                    logWarn("duplicate request exception during createVisit")
                }
                else -> throw ex
            }
        }
        if (draftVisit.isOtherVisit) {
            logWarn("uploaded other visit. Deleting it right away!")
            draftVisitRepository.deleteByVisitUuid(draftVisit.visitUuid)
        } else {
            val uploadedDraftVisit = draftVisit.copy(
                draftState = DraftState.UPLOADED)
            updateDraftStates(uploadedDraftVisit)
        }
    }

}