package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftParticipantBiometricsTemplateFile
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.ui.minus
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.delete.DeleteBiometricsTemplateUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.upload.UploadFailedBiometricsTemplateUseCase.Result
import kotlinx.coroutines.yield
import javax.inject.Inject

class UploadAllFailedBiometricsTemplatesUseCase @Inject constructor(
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val uploadFailedBiometricsTemplateUseCase: UploadFailedBiometricsTemplateUseCase,
    private val deleteBiometricsTemplateUseCase: DeleteBiometricsTemplateUseCase,
    private val syncLogger: SyncLogger,
) {

    @Suppress("MoveVariableDeclarationIntoWhen")
    private suspend fun isDraftParticipantUploaded(template: DraftParticipantBiometricsTemplateFile): Boolean {
        val draftParticipantDraftState = draftParticipantRepository.findDraftStateByParticipantUuid(template.participantUuid)
        return when (draftParticipantDraftState) {
            DraftState.UPLOADED -> true
            null -> true // if draft participant doesn't exist, we assume it's already uploaded
            DraftState.UPLOAD_PENDING -> false
        }
    }

    private suspend fun deleteTemplate(template: DraftParticipantBiometricsTemplateFile) {
        deleteBiometricsTemplateUseCase.delete(template)
    }

    private suspend fun updateTemplateWithDraftState(template: DraftParticipantBiometricsTemplateFile, draftState: DraftState) {
        val newTemplate = template.copy(draftState = draftState, dateLastUploadAttempt = DateEntity())
        draftParticipantBiometricsTemplateRepository.updateDraftState(newTemplate)
    }

    private suspend fun handleResult(template: DraftParticipantBiometricsTemplateFile, result: Result) {
        logInfo("handleResult: $result for template of participant ${template.participantUuid} ${template.fileName}")
        when (result) {
            Result.InvalidTemplate, Result.LocalTemplateNotAvailable, Result.ParticipantNotFound -> {
                deleteTemplate(template)
                syncLogger.clearSyncError(template.toSyncError())
            }
            Result.Success, Result.TemplateAlreadyExists -> {
                updateTemplateWithDraftState(template, DraftState.UPLOADED)
                syncLogger.clearSyncError(template.toSyncError())
            }
            is Result.UnknownWebCallError -> {
                updateTemplateWithDraftState(template, template.draftState)
                syncLogger.logSyncError(template.toSyncError(), result.exception)
            }
        }.let {}
    }

    private fun DraftParticipantBiometricsTemplateFile.toSyncError() = SyncErrorMetadata.UploadBiometricsTemplate(participantUuid = participantUuid)

    suspend fun uploadFailedTemplates(timeSinceLastUploadAttempt: Long) {
        val date = DateEntity() - timeSinceLastUploadAttempt
        val templates = draftParticipantBiometricsTemplateRepository.findAllByDateLastUploadAttemptLesserThan(date)
        templates.forEach { template ->
            if (isDraftParticipantUploaded(template)) {
                try {
                    val result = uploadFailedBiometricsTemplateUseCase.upload(template)
                    handleResult(template, result)
                } catch (ex: Exception) {
                    yield()
                    ex.rethrowIfFatal()
                    syncLogger.logSyncError(template.toSyncError(), ex)
                    throw ex
                }
            } else {
                logInfo("should not upload template: pUuid->${template.participantUuid} fileName->${template.fileName}")
            }
        }
    }
}