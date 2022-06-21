package com.jnj.vaccinetracker.common.data.database.repositories


import com.jnj.vaccinetracker.common.data.database.daos.base.deleteByVisitUuid
import com.jnj.vaccinetracker.common.data.database.daos.base.updateDraftStateOrThrow
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftVisitAttributeDao
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftVisitDao
import com.jnj.vaccinetracker.common.data.database.entities.base.toMap
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitAttributeEntity.Companion.toDraftVisitAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEntity
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftVisitModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateVisitDraftStateModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.DraftVisitRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class DraftVisitRepository @Inject constructor(
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val draftVisitDao: DraftVisitDao,
    private val draftVisitAttributeDao: DraftVisitAttributeDao,
) : DraftVisitRepositoryBase<DraftVisit> {

    private fun RoomDraftVisitModel.toDomain() = DraftVisit(
        visitUuid = visitUuid,
        attributes = attributes.toMap(),
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        visitType = visitType,
        participantUuid = participantUuid,
        draftState = draftState
    )

    private fun DraftVisit.toPersistence() = DraftVisitEntity(
        visitUuid = visitUuid,
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        visitType = visitType,
        draftState = draftState,
        participantUuid = participantUuid
    )

    override fun observeChanges(): Flow<Long> {
        return draftVisitDao.observeChanges()
    }

    override suspend fun updateDraftState(draft: DraftVisit) {
        logInfo("updateDraftState: ${draft.visitUuid} ${draft.draftState}")
        draftVisitDao.updateDraftStateOrThrow(
            RoomUpdateVisitDraftStateModel(visitUuid = draft.visitUuid, draftState = draft.draftState)
        )
    }

    override suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<String> {
        return draftVisitDao.findAllParticipantUuidsByDraftState(draftState, offset, limit).map { it.participantUuid }
    }

    override suspend fun findAllByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): List<DraftVisit> {
        return draftVisitDao.findAllByParticipantUuidAndDraftState(participantUuid, draftState).map { it.toDomain() }
    }

    override suspend fun findAllByParticipantUuid(participantUuid: String): List<DraftVisit> {
        return draftVisitDao.findAllByParticipantUuid(participantUuid).map { it.toDomain() }
    }

    override suspend fun findByVisitUuid(visitUuid: String): DraftVisit? = draftVisitDao.findByVisitUuid(visitUuid)?.toDomain()

    override suspend fun insert(model: DraftVisit, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            if (orReplace) {
                //we assume due to foreign keys, the related child rows will be deleted as well
                val isDeleted = draftVisitDao.deleteByVisitUuid(model.visitUuid) > 0
                if (isDeleted) {
                    logInfo("deleted draft visit for replace ${model.visitUuid}")
                }
            }

            val insertedVisit = draftVisitDao.insert(model.toPersistence()) > 0
            if (!insertedVisit) {
                throw InsertEntityException("Cannot save draft visit: ${model.visitUuid}", orReplace = orReplace)
            }

            val attributes = model.attributes.map { it.toDraftVisitAttributeEntity(model.visitUuid) }
            if (attributes.isNotEmpty()) {
                val insertedAttributes = draftVisitAttributeDao.insertOrReplaceAll(attributes)
                    .all { it > 0 }
                if (!insertedAttributes) {
                    throw InsertEntityException("Cannot save attributes for draft visit: ${model.visitUuid}", orReplace = orReplace)
                }
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save draft visit", orReplace = orReplace)
        }
    }

    override suspend fun deleteAllUploaded() {
        val countDeleted = draftVisitDao.deleteAllByDraftState(DraftState.UPLOADED)
        logInfo("deleteAllUploaded: countDeleted $countDeleted")
    }

    override suspend fun findDraftStateByVisitUuid(visitUuid: String): DraftState? {
        return draftVisitDao.findDraftStateByVisitUuid(visitUuid)
    }

    override suspend fun deleteByVisitUuid(visitUuid: String): Boolean {
        val success = draftVisitDao.deleteByVisitUuid(visitUuid) > 0
        logInfo("deleteByVisitUuid: $visitUuid $success")
        return success
    }

    override suspend fun deleteByVisitUuid(visitUuid: String, draftState: DraftState): Boolean {
        val success = draftVisitDao.deleteByVisitUuid(visitUuid, draftState) > 0
        logInfo("deleteByVisitUuid: $visitUuid $draftState $success")
        return success
    }

    override suspend fun countByDraftState(draftState: DraftState): Long {
        return draftVisitDao.countByDraftState(draftState)
    }
}