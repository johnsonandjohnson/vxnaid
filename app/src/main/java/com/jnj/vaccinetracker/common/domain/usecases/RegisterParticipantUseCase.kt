package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.*
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.upload.UploadDraftParticipantUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegisterParticipantUseCase @Inject constructor(
    private val participantDataFileIO: ParticipantDataFileIO,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val uploadDraftParticipantUseCase: UploadDraftParticipantUseCase,
    private val createVisitUseCase: CreateVisitUseCase,
    private val findParticipantByParticipantIdUseCase: FindParticipantByParticipantIdUseCase,
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val syncLogger: SyncLogger,
) {

    private suspend fun ImageBytes.writeToDisk(participantImageFile: DraftParticipantImageFile) {
        participantDataFileIO.writeParticipantDataFile(participantImageFile, bytes, overwrite = false)
    }

    private suspend fun BiometricsTemplateBytes.writeToDisk(biometricsTemplateFile: DraftParticipantBiometricsTemplateFile) {
        participantDataFileIO.writeParticipantDataFile(biometricsTemplateFile, bytes, overwrite = false)
    }

    private fun RegisterParticipant.toDomain(
        participantUuid: String,
        registrationDate: DateEntity,
        participantBiometricsTemplateFile: DraftParticipantBiometricsTemplateFile?,
        participantImageFile: DraftParticipantImageFile?,
    ) = DraftParticipant(
        participantUuid = participantUuid,
        registrationDate = registrationDate,
        image = participantImageFile,
        biometricsTemplate = participantBiometricsTemplateFile,
        participantId = participantId,
        nin = nin,
        gender = gender,
        birthDate = birthDate,
        attributes = attributes,
        address = address,
        draftState = DraftState.initialState(),
    )

    private suspend fun writeImageToDisk(file: DraftParticipantImageFile, imageBytes: ImageBytes) {
        imageBytes.writeToDisk(file)
    }

    private suspend fun writeBiometricsTemplateToDisk(file: DraftParticipantBiometricsTemplateFile, biometricsTemplateBytes: BiometricsTemplateBytes) {
        biometricsTemplateBytes.writeToDisk(file)
    }

    private fun onParticipantUploaded(draftParticipant: DraftParticipant): Unit {
        logInfo("onParticipantUploaded: ${draftParticipant.participantUuid}")
        draftParticipant.biometricsTemplate?.let { template ->
            if (template.draftState.isPendingUpload()) {
                logInfo("template is still pending upload, so logging sync error")
                // template was not registered so log sync error, we will retry after an hour
                val syncError = SyncErrorMetadata.UploadBiometricsTemplate(participantUuid = draftParticipant.participantUuid)
                syncLogger.logSyncError(syncError, Exception("error, failure uploading templates"))
            }
        }

    }

    private fun ScheduleFirstVisit.toCreateVisit(participantUuid: String) = CreateVisit(
        participantUuid = participantUuid,
        visitType = visitType,
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        attributes = attributes
    )


    suspend fun registerParticipant(registerParticipant: RegisterParticipant): DraftParticipant {
        val existingParticipant = findParticipantByParticipantIdUseCase.findByParticipantId(participantId = registerParticipant.participantId)
        if (existingParticipant != null ) {
            throw ParticipantAlreadyExistsException()
        }
        else{
            val deletedParticipant =  findParticipantByParticipantIdUseCase.findDeletedParticipantbyId(participantId = registerParticipant.participantId)
            if(deletedParticipant !=null)
                throw ParticipantAlreadyExistsException()
        }
        val registrationDate = dateNow()
        val participantUuid = uuid()
        val biometricsFile = registerParticipant.biometricsTemplate?.let { DraftParticipantBiometricsTemplateFile.newFile(participantUuid) }
        val imageFile = registerParticipant.image?.let { DraftParticipantImageFile.newFile(participantUuid) }
        var success = false
        return try {
            biometricsFile?.let { writeBiometricsTemplateToDisk(it, registerParticipant.biometricsTemplate) }
            imageFile?.let { writeImageToDisk(it, registerParticipant.image) }
            var participant = registerParticipant.toDomain(
                participantUuid = participantUuid,
                registrationDate = registrationDate,
                participantBiometricsTemplateFile = biometricsFile,
                participantImageFile = imageFile
            )
            try {
                participant = uploadDraftParticipantUseCase.upload(participant, allowDuplicate = false, updateDraftState = false)
                onParticipantUploaded(participant)
            } catch (ex: NoNetworkException) {
                logWarn("Upload participant failed: No network")
            } catch (ex: ParticipantAlreadyExistsException) {
                logWarn("Upload participant failed: Participant does already exists")
                throw ex
            } catch (ex: Exception) {
                logWarn("Upload participant failed", ex)
            }



            transactionRunner.withTransaction {
                draftParticipantRepository.insert(participant, orReplace = false)
                val createVisit = registerParticipant.scheduleFirstVisit.toCreateVisit(participantUuid)
                createVisitUseCase.createVisit(createVisit)
            }

            success = true
            participant
        } finally {
            if (!success) {
                listOfNotNull(biometricsFile, imageFile).forEach { file ->
                    participantDataFileIO.deleteParticipantDataFile(file)
                }
            }
        }
    }
}