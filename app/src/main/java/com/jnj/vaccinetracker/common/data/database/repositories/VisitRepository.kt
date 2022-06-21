package com.jnj.vaccinetracker.common.data.database.repositories


import com.jnj.vaccinetracker.common.data.database.daos.VisitAttributeDao
import com.jnj.vaccinetracker.common.data.database.daos.VisitDao
import com.jnj.vaccinetracker.common.data.database.daos.VisitObservationDao
import com.jnj.vaccinetracker.common.data.database.daos.base.deleteByVisitUuid
import com.jnj.vaccinetracker.common.data.database.entities.VisitAttributeEntity.Companion.toVisitAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.VisitEntity
import com.jnj.vaccinetracker.common.data.database.entities.VisitObservationEntity.Companion.toVisitObservationEntity
import com.jnj.vaccinetracker.common.data.database.entities.base.toMap
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel.Companion.toDomain
import com.jnj.vaccinetracker.common.data.database.models.RoomVisitModel
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteVisitModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.SyncRepositoryBase
import com.jnj.vaccinetracker.common.data.database.repositories.base.VisitRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class VisitRepository @Inject constructor(
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val visitDao: VisitDao,
    private val visitAttributeDao: VisitAttributeDao,
    private val visitObservationDao: VisitObservationDao,
) : VisitRepositoryBase<Visit>, SyncRepositoryBase {

    private fun RoomVisitModel.toDomain() = Visit(
        visitUuid = visitUuid,
        attributes = attributes.toMap(),
        observations = observations.toMap(),
        dateModified = startDatetime,
        startDatetime = startDatetime,
        visitType = visitType,
        participantUuid = participantUuid,
    )

    private fun Visit.toPersistence() = VisitEntity(
        visitUuid = visitUuid,
        startDatetime = startDatetime,
        visitType = visitType,
        participantUuid = participantUuid,
        dateModified = dateModified
    )

    override suspend fun findMostRecentDateModifiedOccurrence(): DateModifiedOccurrence? = visitDao.findMostRecentDateModifiedOccurrence().toDomain()
    override suspend fun findAllByParticipantUuid(participantUuid: String): List<Visit> {
        return visitDao.findAllByParticipantUuid(participantUuid).map { it.toDomain() }
    }

    override suspend fun deleteByVisitUuid(visitUuid: String): Boolean {
        return visitDao.delete(RoomDeleteVisitModel(visitUuid)).let { countDeleted ->
            logDebug("deleteByVisitUuid: $visitUuid $countDeleted")
            countDeleted > 0
        }
    }

    override suspend fun findByVisitUuid(visitUuid: String): Visit? = visitDao.findByVisitUuid(visitUuid)?.toDomain()

    override suspend fun insert(model: Visit, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            if (orReplace) {
                //we assume due to foreign keys, the related child rows will be deleted as well
                val isDeleted = visitDao.deleteByVisitUuid(model.visitUuid) > 0
                if (isDeleted) {
                    logInfo("deleted visit for replace ${model.visitUuid}")
                }
            }

            val insertedVisit = visitDao.insert(model.toPersistence()) > 0
            if (!insertedVisit) {
                throw InsertEntityException("Cannot save visit: ${model.visitUuid}", orReplace = orReplace)
            }

            val attributes = model.attributes.map { it.toVisitAttributeEntity(model.visitUuid) }
            if (attributes.isNotEmpty()) {
                val insertedAttributes = visitAttributeDao.insertOrReplaceAll(attributes)
                    .all { it > 0 }
                if (!insertedAttributes) {
                    throw InsertEntityException("Cannot save attributes for visit: ${model.visitUuid}", orReplace = orReplace)
                }
            }

            val observations = model.observations.map { it.toVisitObservationEntity(model.visitUuid) }
            if (observations.isNotEmpty()) {
                val insertedObservations = visitObservationDao.insertOrReplaceAll(observations)
                    .all { it > 0 }
                if (!insertedObservations) {
                    throw InsertEntityException("Cannot save observations for visit: ${model.visitUuid}", orReplace = orReplace)
                }
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save visit", orReplace = orReplace)
        }
    }

    override suspend fun deleteAll() {
        visitDao.deleteAll()
    }

    override suspend fun count(): Long {
        // for compatibility with previous production release.
        // Otherwise they would get a sync error since backend now only returns dosing visits
        return visitDao.countByVisitType(Constants.VISIT_TYPE_DOSING)
    }

    override fun observeChanges(): Flow<Long> {
        return visitDao.observeChanges()
    }
}