package com.jnj.vaccinetracker.common.data.database.repositories


import com.jnj.vaccinetracker.common.data.database.daos.base.deleteByVisitUuid
import com.jnj.vaccinetracker.common.data.database.daos.base.updateDraftStateOrThrow
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftVisitEncounterAttributeDao
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftVisitEncounterDao
import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftVisitEncounterObservationDao
import com.jnj.vaccinetracker.common.data.database.entities.base.toMap
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterAttributeEntity.Companion.toDraftVisitEncounterAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterObservationEntity.Companion.toDraftVisitEncounterObservationEntity
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftVisitEncounterModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateVisitDraftStateModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.DraftVisitRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class DraftVisitEncounterRepository @Inject constructor(
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val draftVisitEncounterDao: DraftVisitEncounterDao,
    private val draftVisitEncounterAttributeDao: DraftVisitEncounterAttributeDao,
    private val draftVisitEncounterObservationDao: DraftVisitEncounterObservationDao,
) : DraftVisitRepositoryBase<DraftVisitEncounter> {

    private fun RoomDraftVisitEncounterModel.toDomain() = DraftVisitEncounter(
        visitUuid = visitUuid,
        attributes = attributes.toMap(),
        observations = observations.toMap(),
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        draftState = draftState,
        participantUuid = participantUuid
    )

    override suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<String> {
        return draftVisitEncounterDao.findAllParticipantUuidsByDraftState(draftState, offset, limit).map { it.participantUuid }
    }

    private fun DraftVisitEncounter.toPersistence() = DraftVisitEncounterEntity(
        visitUuid = visitUuid,
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        draftState = DraftState.initialState(),
        participantUuid = participantUuid
    )

    override fun observeChanges(): Flow<Long> {
        return draftVisitEncounterDao.observeChanges()
    }

    override suspend fun findAllByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): List<DraftVisitEncounter> {
        return draftVisitEncounterDao.findAllByParticipantUuidAndDraftState(participantUuid, draftState).map { it.toDomain() }
    }

    override suspend fun findAllByParticipantUuid(participantUuid: String): List<DraftVisitEncounter> {
        return draftVisitEncounterDao.findAllByParticipantUuid(participantUuid).map { it.toDomain() }
    }

    override suspend fun updateDraftState(draft: DraftVisitEncounter) {
        draftVisitEncounterDao.updateDraftStateOrThrow(
            RoomUpdateVisitDraftStateModel(visitUuid = draft.visitUuid, draftState = draft.draftState)
        )
    }

    override suspend fun findByVisitUuid(visitUuid: String): DraftVisitEncounter? = draftVisitEncounterDao.findByVisitUuid(visitUuid)?.toDomain()

    override suspend fun insert(model: DraftVisitEncounter, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            if (orReplace) {
                //we assume due to foreign keys, the related child rows will be deleted as well
                val isDeleted = draftVisitEncounterDao.deleteByVisitUuid(model.visitUuid) > 0
                if (isDeleted) {
                    logInfo("deleted draft visit encounter for replace ${model.visitUuid}")
                }
            }

            val insertedVisit = draftVisitEncounterDao.insert(model.toPersistence()) > 0
            if (!insertedVisit) {
                throw InsertEntityException("Cannot save draft visit encounter: ${model.visitUuid}", orReplace = orReplace)
            }

            val attributes = model.attributes.map { it.toDraftVisitEncounterAttributeEntity(model.visitUuid) }
            if (attributes.isNotEmpty()) {
                val insertedAttributes = draftVisitEncounterAttributeDao.insertOrReplaceAll(attributes)
                    .all { it > 0 }
                if (!insertedAttributes) {
                    throw InsertEntityException("Cannot save attributes for draft visit encounter: ${model.visitUuid}", orReplace = orReplace)
                }
            }

            val observations = model.observations.map { it.toDraftVisitEncounterObservationEntity(model.visitUuid) }
            if (observations.isNotEmpty()) {
                val insertedObservations = draftVisitEncounterObservationDao.insertOrReplaceAll(observations)
                    .all { it > 0 }
                if (!insertedObservations) {
                    throw InsertEntityException("Cannot save observations for draft visit encounter: ${model.visitUuid}", orReplace = orReplace)
                }
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save draft visit encounter", orReplace = orReplace)
        }
    }

    override suspend fun deleteAllUploaded() {
        val countDeleted = draftVisitEncounterDao.deleteAllByDraftState(DraftState.UPLOADED)
        logInfo("deleteAllUploaded: countDeleted $countDeleted")
    }

    override suspend fun findDraftStateByVisitUuid(visitUuid: String): DraftState? {
        return draftVisitEncounterDao.findDraftStateByVisitUuid(visitUuid)
    }

    override suspend fun deleteByVisitUuid(visitUuid: String, draftState: DraftState): Boolean {
        val success = draftVisitEncounterDao.deleteByVisitUuid(visitUuid, draftState) > 0
        logInfo("deleteByVisitUuid: $visitUuid $draftState $success")
        return success
    }

    override suspend fun deleteByVisitUuid(visitUuid: String): Boolean {
        val success = draftVisitEncounterDao.deleteByVisitUuid(visitUuid) > 0
        logInfo("deleteByVisitUuid: $visitUuid $success")
        return success
    }

    override suspend fun countByDraftState(draftState: DraftState): Long {
        return draftVisitEncounterDao.countByDraftState(draftState)
    }
}