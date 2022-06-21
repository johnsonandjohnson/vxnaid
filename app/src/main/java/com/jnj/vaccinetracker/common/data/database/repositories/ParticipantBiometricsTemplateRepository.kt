package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.ParticipantBiometricsTemplateDao
import com.jnj.vaccinetracker.common.data.database.daos.base.findAll
import com.jnj.vaccinetracker.common.data.database.daos.base.findAllByPhoneNullable
import com.jnj.vaccinetracker.common.data.database.daos.base.forEachAll
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantBiometricsEntity
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel.Companion.toDomain
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.BiometricsTemplateRepositoryBase
import com.jnj.vaccinetracker.common.data.database.repositories.base.SyncRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFile
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class ParticipantBiometricsTemplateRepository @Inject constructor(
    private val participantBiometricsTemplateDao: ParticipantBiometricsTemplateDao,
    private val transactionRunner: ParticipantDbTransactionRunner,
) : BiometricsTemplateRepositoryBase<ParticipantBiometricsTemplateFile>, SyncRepositoryBase {

    private fun ParticipantBiometricsEntity.toDomain() = ParticipantBiometricsTemplateFile(
        participantUuid = participantUuid,
        fileName = biometricsTemplateFileName,
        dateModified = dateModified,
    )

    private fun ParticipantBiometricsTemplateFile.toPersistence() = ParticipantBiometricsEntity(
        participantUuid = participantUuid,
        biometricsTemplateFileName = fileName,
        dateModified = dateModified
    )

    override suspend fun forEachAll(onPageResult: suspend (List<ParticipantBiometricsTemplateFile>) -> Unit) {
        participantBiometricsTemplateDao.forEachAll { entities ->
            val items = entities.map { it.toDomain() }
            onPageResult(items)
        }
    }

    override suspend fun findAll(): List<ParticipantBiometricsTemplateFile> {
        logInfo("findAll")
        return participantBiometricsTemplateDao.findAll().map { it.toDomain() }
    }

    override fun observeChanges(): Flow<Long> {
        return participantBiometricsTemplateDao.observeChanges()
    }

    override suspend fun findByParticipantUuid(participantUuid: String): ParticipantBiometricsTemplateFile? {
        return participantBiometricsTemplateDao.findByParticipantUuid(participantUuid)?.toDomain()
    }

    override suspend fun findMostRecentDateModifiedOccurrence(): DateModifiedOccurrence? = participantBiometricsTemplateDao.findMostRecentDateModifiedOccurrence().toDomain()

    override suspend fun deleteAll() {
        participantBiometricsTemplateDao.deleteAll()
    }

    suspend fun deleteByParticipantUuid(participantUuid: String): Int {
        return participantBiometricsTemplateDao.delete(RoomDeleteParticipantModel(participantUuid)).also { countDeleted ->
            logDebug("deleteByParticipantUuid: $participantUuid $countDeleted")
        }
    }

    override suspend fun findAllByPhone(phone: String?): List<ParticipantBiometricsTemplateFile> {
        return participantBiometricsTemplateDao.findAllByPhoneNullable(phone).map { it.toDomain() }
    }

    override suspend fun findByParticipantId(participantId: String): ParticipantBiometricsTemplateFile? {
        return participantBiometricsTemplateDao.findByParticipantId(participantId)?.toDomain()
    }

    override suspend fun insert(model: ParticipantBiometricsTemplateFile, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            val entity = model.toPersistence()
            val insertedIrisTemplate = (if (orReplace) participantBiometricsTemplateDao.insertOrReplace(entity) else participantBiometricsTemplateDao.insert(entity)) > 0
            if (!insertedIrisTemplate) {
                throw InsertEntityException("Cannot save participant irisTemplate: ${model.participantUuid}", orReplace = orReplace)
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save participant irisTemplate", orReplace = orReplace)
        }
    }

}