package com.jnj.vaccinetracker.sync.domain.usecases.store

import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFile
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.ParticipantBiometricsTemplateSyncRecord
import com.jnj.vaccinetracker.sync.data.models.ParticipantBiometricsTemplateSyncRecord.Delete.Companion.toDomain
import com.jnj.vaccinetracker.sync.domain.usecases.delete.DeleteBiometricsTemplateUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.base.StoreSyncRecordUseCaseBase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreParticipantBiometricsTemplateSyncRecordUseCase @Inject constructor(
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
    private val participantDataFileIO: ParticipantDataFileIO,
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val base64: Base64,
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val deleteBiometricsTemplateUseCase: DeleteBiometricsTemplateUseCase,
) : StoreSyncRecordUseCaseBase<ParticipantBiometricsTemplateSyncRecord, ParticipantBiometricsTemplateFile> {

    private suspend fun BiometricsTemplateBytes.writeToDisk(participantBiometricsTemplateFile: ParticipantBiometricsTemplateFile, overwrite: Boolean) {
        participantDataFileIO.writeParticipantDataFile(participantBiometricsTemplateFile, bytes, overwrite = overwrite)
    }

    private suspend fun onInsertSuccess(participantUuid: String) {
        logDebug("onInsertSuccess $participantUuid")
        val draftTemplate = draftParticipantBiometricsTemplateRepository.findByParticipantUuid(participantUuid)
        if (draftTemplate != null) {
            val isFileDeleted = participantDataFileIO.deleteParticipantDataFile(draftTemplate)
            val isRecordDeleted = draftParticipantBiometricsTemplateRepository.deleteByParticipantUuid(participantUuid)
            logInfo("delete draft biometrics template [draftState:${draftTemplate.draftState}, isFileDeleted:$isFileDeleted, isRecordDeleted:$isRecordDeleted]")
        }
    }

    private suspend fun update(syncRecord: ParticipantBiometricsTemplateSyncRecord.Update) {
        val participantUuid = syncRecord.participantUuid

        /**
         * For privacy reasons we generate a unique uuid for each file which is not used anywhere else.
         * This means that if we store the same sync record twice we'll create a new file for each record. Wasting valuable disk space.
         * So if there's already a syncRecord associated with the template then reuse it.
         * If the template does not exists we do not need overwrite.
         */
        val existingTemplate = participantBiometricsTemplateRepository.findByParticipantUuid(participantUuid)
        val dstBiometricsTemplateFile = existingTemplate?.copy(dateModified = syncRecord.dateModified.date) ?: ParticipantBiometricsTemplateFile.newFile(
            participantUuid = participantUuid,
            dateModified = syncRecord.dateModified.date,
        )
        var writeSuccess = false
        var insertSuccess = false
        try {
            val bytes = BiometricsTemplateBytes(syncRecord.biometricsTemplate.let { base64.decode(it) })
            bytes.writeToDisk(dstBiometricsTemplateFile, overwrite = existingTemplate != null)
            writeSuccess = true
            participantBiometricsTemplateRepository.insert(dstBiometricsTemplateFile, orReplace = true)
            insertSuccess = true
            onInsertSuccess(participantUuid)
        } finally {
            if (!insertSuccess && writeSuccess) {
                // only delete the file if we wrote the file successfully but failed to insert
                participantDataFileIO.deleteParticipantDataFile(dstBiometricsTemplateFile)
            }
        }
    }

    private suspend fun delete(syncRecord: ParticipantBiometricsTemplateSyncRecord.Delete) {
        participantBiometricsTemplateRepository.findByParticipantUuid(syncRecord.participantUuid)?.let { template ->
            deleteBiometricsTemplateUseCase.delete(template)
        }
        draftParticipantBiometricsTemplateRepository.findByParticipantUuid(syncRecord.participantUuid)?.let { template ->
            deleteBiometricsTemplateUseCase.delete(template)
        }
        deletedSyncRecordRepository.insert(syncRecord.toDomain(), orReplace = true)
    }

    override suspend fun store(syncRecord: ParticipantBiometricsTemplateSyncRecord) = transactionRunner.withTransaction {
        when (syncRecord) {
            is ParticipantBiometricsTemplateSyncRecord.Delete -> delete(syncRecord)
            is ParticipantBiometricsTemplateSyncRecord.Update -> update(syncRecord)
        }.let { }
    }
}