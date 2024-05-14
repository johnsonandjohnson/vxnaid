package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.database.mappers.toDto
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.data.models.api.request.RegisterParticipantRequest
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.data.models.api.response.RegisterParticipantResponse
import com.jnj.vaccinetracker.common.data.models.toDto
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.DuplicateRequestException
import com.jnj.vaccinetracker.common.exceptions.ParticipantAlreadyExistsException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.sync.data.models.GetBiometricsTemplatesByUuidsRequest
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.usecases.IsDraftParticipantIrrelevantUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.MakeParticipantIdUniqueUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.delete.DeleteBiometricsTemplateUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.delete.DeleteImageUseCase
import java.util.*
import javax.inject.Inject

class UploadDraftParticipantUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantDataFileIO: ParticipantDataFileIO,
    private val base64: Base64,
    private val isDraftParticipantIrrelevantUseCase: IsDraftParticipantIrrelevantUseCase,
    private val makeParticipantIdUniqueUseCase: MakeParticipantIdUniqueUseCase,
    private val deleteBiometricsTemplateUseCase: DeleteBiometricsTemplateUseCase,
    private val deleteImageUseCase: DeleteImageUseCase,
) {

    suspend fun upload(draftParticipant: DraftParticipant, allowDuplicate: Boolean, updateDraftState: Boolean): DraftParticipant = uploadImpl(
        draftParticipant = draftParticipant,
        isMadeUnique = false,
        allowDuplicate = allowDuplicate,
        updateDraftState = updateDraftState
    )

    private suspend fun uploadImpl(draftParticipant: DraftParticipant, isMadeUnique: Boolean, allowDuplicate: Boolean, updateDraftState: Boolean): DraftParticipant {
        require(draftParticipant.draftState.isPendingUpload()) { "Participant already uploaded!" }
        logInfo("uploadImpl ${draftParticipant.participantId} isMadeUnique: $isMadeUnique allowDuplicate: $allowDuplicate UpdateDraftState: $updateDraftState")
        val image = draftParticipant.readImageBase64()
        val request = draftParticipant.toDto(imageBase64 = image)
        val response = try {
            api.registerParticipant(request, draftParticipant.biometricsTemplate?.readBytes())
        } catch (ex: WebCallException) {
            when (ex.cause) {
                is DuplicateRequestException -> onDuplicateRequestException(draftParticipant)
                is ParticipantAlreadyExistsException -> return ex.cause.onParticipantIdAlreadyExists(draftParticipant, isMadeUnique, allowDuplicate, updateDraftState)
                else -> throw ex
            }
        }
        if (!response.isIrisRegistered && draftParticipant.biometricsTemplate != null) {
            logWarn("participant ${draftParticipant.participantUuid} registered successfully but iris couldn't be stored")
        } else {
            logInfo("participant ${draftParticipant.participantUuid} registered successfully")
        }
        val isTemplateRegistered = response.isIrisRegistered
        val uploadedDraftParticipant = draftParticipant.markUploaded(isTemplateRegistered = isTemplateRegistered)
        val isIrrelevant = isDraftParticipantIrrelevantUseCase.isIrrelevant(uploadedDraftParticipant)
        if (isIrrelevant) {
            logInfo("uploaded draft participant ${draftParticipant.participantUuid} is irrelevant, so deleting it")
            deleteAll(uploadedDraftParticipant, deleteTemplate = isTemplateRegistered)
        } else if (updateDraftState) {
            updateDraftStates(uploadedDraftParticipant, updateBiometricsTemplate = true)
        }
        return uploadedDraftParticipant
    }

    private suspend fun DraftParticipant.readImageBase64(): String? {
        return if (image != null) {
            participantDataFileIO.readParticipantDataFileContent(image)?.let { base64.encode(it) }
        } else null
    }

    private suspend fun DraftParticipantBiometricsTemplateFile.readBytes() = participantDataFileIO.readParticipantDataFileContent(this)?.let { BiometricsTemplateBytes(it) }

    private fun DraftParticipant.toDto(imageBase64: String?) = RegisterParticipantRequest(
        participantId = participantId,
        nin = nin,
        gender = gender,
        birthdate = birthDate.toDto(),
        addresses = listOfNotNull(address?.toDto()),
        attributes = attributes.map { AttributeDto(it.key, it.value) },
        image = imageBase64,
        registrationDate = registrationDate,
        participantUuid = participantUuid
    )

    private fun DraftParticipant.markUploaded(isTemplateRegistered: Boolean): DraftParticipant {
        val newDraftState = DraftState.UPLOADED
        val template = biometricsTemplate
            ?.copy(dateLastUploadAttempt = Date())
            ?.let { if (isTemplateRegistered) it.copy(draftState = newDraftState) else it }

        return copy(
            draftState = newDraftState,
            image = image?.copy(draftState = newDraftState),
            biometricsTemplate = template)
    }

    /**
     * always set biometrics template draft state to uploaded even if iris wasn't registered
     */
    private suspend fun updateDraftStates(draftParticipant: DraftParticipant, updateBiometricsTemplate: Boolean) {
        draftParticipantRepository.updateDraftState(draftParticipant, updateImage = true, updateBiometricsTemplate = updateBiometricsTemplate)
    }

    private suspend fun deleteAll(draftParticipant: DraftParticipant, deleteTemplate: Boolean) {
        require(draftParticipant.draftState.isUploaded()) { "draftParticipant must be uploaded before deletion" }
        draftParticipant.image?.let { deleteImageUseCase.delete(it) }
        if (deleteTemplate)
            draftParticipant.biometricsTemplate?.let { deleteBiometricsTemplateUseCase.delete(it) }
        draftParticipantRepository.deleteByParticipantUuid(draftParticipant.participantUuid)
    }

    private suspend fun ParticipantAlreadyExistsException.onParticipantIdAlreadyExists(
        draftParticipant: DraftParticipant,
        isMadeUnique: Boolean,
        allowDuplicate: Boolean,
        updateDraftState: Boolean,
    ): DraftParticipant {
        logWarn("onParticipantIdAlreadyExists: ${draftParticipant.participantUuid} ${draftParticipant.participantId} $isMadeUnique")
        if (!allowDuplicate) throw this

        if (isMadeUnique) {
            logError("Failed to upload made unique participant. Rethrowing exception. Will automatically retry later.")
            throw ParticipantAlreadyExistsException("made unique participantId ${draftParticipant.participantId} already exists [Original=${draftParticipant.originalParticipantId}]")
        }
        val originalParticipantId = draftParticipant.originalParticipantId ?: draftParticipant.participantId
        val participantIdUnique = makeParticipantIdUniqueUseCase.makeParticipantIdUnique(originalParticipantId)
        val attributes = draftParticipant.attributes.withOriginalParticipantId(originalParticipantId)
        val uniqueDraftParticipant = draftParticipant.copy(participantId = participantIdUnique, attributes = attributes)
        draftParticipantRepository.updateDraftParticipant(uniqueDraftParticipant)
        logInfo("ParticipantAlreadyExistsException. Made participant unique, retrying upload: ${uniqueDraftParticipant.participantUuid} ${uniqueDraftParticipant.participantId}")
        return uploadImpl(uniqueDraftParticipant, isMadeUnique = true, allowDuplicate, updateDraftState)
    }

    private suspend fun onDuplicateRequestException(draftParticipant: DraftParticipant): RegisterParticipantResponse {
        // ignore exception and pretend registration was done so draft state becomes uploaded
        logWarn("duplicate request exception during registerParticipant")
        return try {
            val templateResponse = api.getBiometricsTemplatesByUuids(GetBiometricsTemplatesByUuidsRequest(listOf(draftParticipant.participantUuid)))
            val isTemplateRegistered = templateResponse.any { it.participantUuid == draftParticipant.participantUuid }
            RegisterParticipantResponse(patientUuid = draftParticipant.participantUuid, isIrisRegistered = isTemplateRegistered)
        } catch (ex: Exception) {
            logError("failed to fetch uploaded template for ${draftParticipant.participantUuid}", ex)
            throw Exception("Got DuplicateRequestException but failed to verify whether template was registered", ex)
        }
    }


}