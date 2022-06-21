package com.jnj.vaccinetracker.sync.domain.usecases.store

import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantImageRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.domain.entities.ImageBytes
import com.jnj.vaccinetracker.common.domain.entities.ParticipantImageFile
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.ParticipantImageSyncRecord
import com.jnj.vaccinetracker.sync.data.models.ParticipantImageSyncRecord.Delete.Companion.toDomain
import com.jnj.vaccinetracker.sync.domain.usecases.delete.DeleteImageUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.base.StoreSyncRecordUseCaseBase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreParticipantImageSyncRecordUseCase @Inject constructor(
    private val participantImageRepository: ParticipantImageRepository,
    private val participantDataFileIO: ParticipantDataFileIO,
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val base64: Base64,
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
    private val deleteImageUseCase: DeleteImageUseCase,
) : StoreSyncRecordUseCaseBase<ParticipantImageSyncRecord, ParticipantImageFile> {

    private suspend fun ImageBytes.writeToDisk(participantImageFile: ParticipantImageFile, overwrite: Boolean) {
        participantDataFileIO.writeParticipantDataFile(participantImageFile, bytes, overwrite = overwrite)
    }

    private suspend fun onInsertSuccess(participantUuid: String) {
        logDebug("onInsertSuccess $participantUuid")
        // in bandwidth saving is turned in, this will be NULL
        val draftImage = draftParticipantImageRepository.findByParticipantUuid(participantUuid)
        if (draftImage != null) {
            val isFileDeleted = participantDataFileIO.deleteParticipantDataFile(draftImage)
            val isRecordDeleted = draftParticipantImageRepository.deleteByParticipantUuid(participantUuid)
            logInfo("delete draft image [draftState:${draftImage.draftState}, isFileDeleted:$isFileDeleted, isRecordDeleted:$isRecordDeleted]")
        }
    }

    private suspend fun update(syncRecord: ParticipantImageSyncRecord.Update) {
        val participantUuid = syncRecord.participantUuid
        val existingImage = participantImageRepository.findByParticipantUuid(participantUuid)

        /**
         * For privacy reasons we generate a unique uuid for each file which is not used anywhere else.
         * This means that if we store the same sync record twice we'll create a new file for each record. Wasting valuable disk space.
         * So if there's already a syncRecord associated with the template then reuse it.
         */
        val dstImageFile = existingImage?.copy(dateModified = syncRecord.dateModified.date) ?: ParticipantImageFile.newFile(
            participantUuid = participantUuid,
            dateModified = syncRecord.dateModified.date,
        )
        var writeSuccess = false
        var insertSuccess = false
        try {
            val bytes = ImageBytes(syncRecord.image.let { base64.decode(it) })
            bytes.writeToDisk(dstImageFile, overwrite = existingImage != null)
            writeSuccess = true
            participantImageRepository.insert(dstImageFile, orReplace = true)
            insertSuccess = true
            onInsertSuccess(participantUuid)
        } finally {
            if (!insertSuccess && writeSuccess) {
                // only delete the file if we wrote the file successfully but failed to insert
                participantDataFileIO.deleteParticipantDataFile(dstImageFile)
            }
        }
    }

    private suspend fun delete(syncRecord: ParticipantImageSyncRecord.Delete) {
        participantImageRepository.findByParticipantUuid(syncRecord.participantUuid)?.let { image ->
            deleteImageUseCase.delete(image)
        }
        draftParticipantImageRepository.findByParticipantUuid(syncRecord.participantUuid)?.let { image ->
            deleteImageUseCase.delete(image)
        }
        deletedSyncRecordRepository.insert(syncRecord.toDomain(), orReplace = true)
    }

    override suspend fun store(syncRecord: ParticipantImageSyncRecord) = transactionRunner.withTransaction {
        when (syncRecord) {
            is ParticipantImageSyncRecord.Delete -> delete(syncRecord)
            is ParticipantImageSyncRecord.Update -> update(syncRecord)
        }.let { }
    }
}