package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.base.forEachAll
import com.jnj.vaccinetracker.common.data.database.daos.base.updateDraftStateOrThrow
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftParticipantImageDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantImageEntity
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.DeleteByDraftParticipant
import com.jnj.vaccinetracker.common.data.database.repositories.base.DraftParticipantDataFileRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.domain.entities.DraftParticipantImageFile
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class DraftParticipantImageRepository @Inject constructor(
    private val draftParticipantImageDao: DraftParticipantImageDao,
    private val transactionRunner: ParticipantDbTransactionRunner,
) : DraftParticipantDataFileRepositoryBase<DraftParticipantImageFile>, DeleteByDraftParticipant {

    private fun DraftParticipantImageEntity.toDomain() = DraftParticipantImageFile(
        participantUuid = participantUuid,
        fileName = imageFileName,
        draftState = draftState
    )

    private fun DraftParticipantImageFile.toPersistence() = DraftParticipantImageEntity(
        participantUuid = participantUuid,
        imageFileName = fileName,
        draftState = draftState
    )

    override fun observeChanges(): Flow<Long> {
        return draftParticipantImageDao.observeChanges()
    }

    override suspend fun updateDraftState(draft: DraftParticipantImageFile) {
        draftParticipantImageDao.updateDraftStateOrThrow(RoomUpdateParticipantDraftStateModel(participantUuid = draft.participantUuid, draftState = draft.draftState))
    }

    override suspend fun forEachAll(onPageResult: suspend (List<DraftParticipantImageFile>) -> Unit) {
        draftParticipantImageDao.forEachAll { entities ->
            val items = entities.map { it.toDomain() }
            onPageResult(items)
        }
    }

    override suspend fun deleteByParticipantUuid(participantUuid: String): Boolean {
        val success = draftParticipantImageDao.delete(RoomDeleteParticipantModel(participantUuid)) > 0
        logInfo("deleteByParticipantId: $success")
        return success
    }

    override suspend fun findByParticipantUuid(participantUuid: String): DraftParticipantImageFile? {
        return draftParticipantImageDao.findByParticipantUuid(participantUuid)?.toDomain()
    }

    override suspend fun insert(model: DraftParticipantImageFile, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            val entity = model.toPersistence()
            val insertedImage = (if (orReplace) draftParticipantImageDao.insertOrReplace(entity) else draftParticipantImageDao.insert(entity)) > 0
            if (!insertedImage) {
                throw InsertEntityException("Cannot save draft participant image: ${model.participantUuid}", orReplace = orReplace)
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save draft participant image", orReplace = orReplace)
        }
    }

    override suspend fun deleteAllUploaded() {
        val countDeleted = draftParticipantImageDao.deleteAllByDraftState(DraftState.UPLOADED)
        logInfo("deleteAllUploaded: countDeleted $countDeleted")
    }

    override suspend fun findAllUploaded(offset: Int, limit: Int): List<DraftParticipantImageFile> {
        return draftParticipantImageDao.findAllByDraftState(DraftState.UPLOADED, offset, limit).map { it.toDomain() }
    }

    override suspend fun countByDraftState(draftState: DraftState): Long {
        return draftParticipantImageDao.countByDraftState(draftState)
    }
}