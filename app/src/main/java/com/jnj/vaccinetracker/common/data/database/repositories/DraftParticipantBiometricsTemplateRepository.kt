package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.base.deleteByParticipantUuid
import com.jnj.vaccinetracker.common.data.database.daos.base.findAll
import com.jnj.vaccinetracker.common.data.database.daos.base.findAllByPhoneNullable
import com.jnj.vaccinetracker.common.data.database.daos.base.forEachAll
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftParticipantBiometricsTemplateDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantBiometricsEntity
import com.jnj.vaccinetracker.common.data.database.helpers.pagingQueryList
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateWithDateModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.DeleteByDraftParticipant
import com.jnj.vaccinetracker.common.data.database.repositories.base.DraftBiometricsTemplateRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftParticipantBiometricsTemplateFile
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class DraftParticipantBiometricsTemplateRepository @Inject constructor(
    private val draftParticipantBiometricsTemplateDao: DraftParticipantBiometricsTemplateDao,
    private val transactionRunner: ParticipantDbTransactionRunner,
) : DraftBiometricsTemplateRepositoryBase<DraftParticipantBiometricsTemplateFile>, DeleteByDraftParticipant {

    private fun DraftParticipantBiometricsEntity.toDomain() = DraftParticipantBiometricsTemplateFile(
        participantUuid = participantUuid,
        fileName = biometricsTemplateFileName,
        draftState = draftState,
        dateLastUploadAttempt = dateLastUploadAttempt
    )

    private fun DraftParticipantBiometricsTemplateFile.toPersistence() = DraftParticipantBiometricsEntity(
        participantUuid = participantUuid,
        biometricsTemplateFileName = fileName,
        draftState = draftState,
        dateLastUploadAttempt = dateLastUploadAttempt
    )

    override suspend fun findAll(): List<DraftParticipantBiometricsTemplateFile> {
        logInfo("findAll")
        return draftParticipantBiometricsTemplateDao.findAll().map { it.toDomain() }
    }

    override suspend fun forEachAll(onPageResult: suspend (List<DraftParticipantBiometricsTemplateFile>) -> Unit) {
        draftParticipantBiometricsTemplateDao.forEachAll { entities ->
            val items = entities.map { it.toDomain() }
            onPageResult(items)
        }
    }

    suspend fun findAllByDateLastUploadAttemptLesserThan(date: DateEntity): List<DraftParticipantBiometricsTemplateFile> {
        return pagingQueryList(pageSize = 5_000) { offset, limit ->
            draftParticipantBiometricsTemplateDao.findAllByDateLastUploadAttemptLesserThan(date, DraftState.UPLOAD_PENDING, offset, limit)
        }.map { it.toDomain() }
    }

    override fun observeChanges(): Flow<Long> {
        return draftParticipantBiometricsTemplateDao.observeChanges()
    }

    override suspend fun findByParticipantUuid(participantUuid: String): DraftParticipantBiometricsTemplateFile? {
        return draftParticipantBiometricsTemplateDao.findByParticipantUuid(participantUuid)?.toDomain()
    }

    override suspend fun updateDraftState(draft: DraftParticipantBiometricsTemplateFile) {
        draftParticipantBiometricsTemplateDao.updateDraftStateWithDate(
            RoomUpdateParticipantDraftStateWithDateModel(participantUuid = draft.participantUuid,
                draftState = draft.draftState,
                dateLastUploadAttempt = draft.dateLastUploadAttempt)
        )
    }

    override suspend fun findAllByPhone(phone: String?): List<DraftParticipantBiometricsTemplateFile> {
        return draftParticipantBiometricsTemplateDao.findAllByPhoneNullable(phone).map { it.toDomain() }
    }

    override suspend fun findByParticipantId(participantId: String): DraftParticipantBiometricsTemplateFile? {
        return draftParticipantBiometricsTemplateDao.findByParticipantId(participantId)?.toDomain()
    }

    override suspend fun deleteByParticipantUuid(participantUuid: String): Boolean {
        val success = draftParticipantBiometricsTemplateDao.deleteByParticipantUuid(participantUuid) > 0
        logInfo("deleteByParticipantId: $success")
        return success
    }

    override suspend fun insert(model: DraftParticipantBiometricsTemplateFile, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            val entity = model.toPersistence()
            val insertedIrisTemplate = (if (orReplace) draftParticipantBiometricsTemplateDao.insertOrReplace(entity) else draftParticipantBiometricsTemplateDao.insert(entity)) > 0
            if (!insertedIrisTemplate) {
                throw InsertEntityException("Cannot save draft participant irisTemplate: ${model.participantUuid}", orReplace = orReplace)
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save draft participant irisTemplate", orReplace = orReplace)
        }
    }

    override suspend fun deleteAllUploaded() {
        val countDeleted = draftParticipantBiometricsTemplateDao.deleteAllByDraftState(DraftState.UPLOADED)
        logInfo("deleteAllUploaded: countDeleted $countDeleted")
    }

    override suspend fun findAllUploaded(offset: Int, limit: Int): List<DraftParticipantBiometricsTemplateFile> {
        return draftParticipantBiometricsTemplateDao.findAllByDraftState(DraftState.UPLOADED, offset, limit).map { it.toDomain() }
    }


    override suspend fun countByDraftState(draftState: DraftState): Long {
        return draftParticipantBiometricsTemplateDao.countByDraftState(draftState)
    }

}