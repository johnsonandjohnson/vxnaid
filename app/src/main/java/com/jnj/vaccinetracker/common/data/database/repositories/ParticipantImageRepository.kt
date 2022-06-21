package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.ParticipantImageDao
import com.jnj.vaccinetracker.common.data.database.daos.base.forEachAll
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantImageEntity
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel.Companion.toDomain
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.ParticipantDataFileRepositoryBase
import com.jnj.vaccinetracker.common.data.database.repositories.base.SyncRepositoryBase
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.ParticipantImageFile
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import javax.inject.Inject

class ParticipantImageRepository @Inject constructor(
    private val participantImageDao: ParticipantImageDao,
    private val transactionRunner: ParticipantDbTransactionRunner,
) : ParticipantDataFileRepositoryBase<ParticipantImageFile>, SyncRepositoryBase {

    private fun ParticipantImageEntity.toDomain() = ParticipantImageFile(
        participantUuid = participantUuid,
        fileName = imageFileName,
        dateModified = dateModified,
    )

    private fun ParticipantImageFile.toPersistence() = ParticipantImageEntity(
        participantUuid = participantUuid,
        imageFileName = fileName,
        dateModified = dateModified
    )

    override suspend fun findByParticipantUuid(participantUuid: String): ParticipantImageFile? {
        return participantImageDao.findByParticipantUuid(participantUuid)?.toDomain()
    }

    suspend fun deleteByParticipantUuid(participantUuid: String): Int {
        return participantImageDao.delete(RoomDeleteParticipantModel(participantUuid)).also { countDeleted ->
            logDebug("deleteByParticipantUuid: $participantUuid $countDeleted")
        }
    }

    override suspend fun forEachAll(onPageResult: suspend (List<ParticipantImageFile>) -> Unit) {
        participantImageDao.forEachAll { entities ->
            val items = entities.map { it.toDomain() }
            onPageResult(items)
        }
    }

    override suspend fun findMostRecentDateModifiedOccurrence(): DateModifiedOccurrence? = participantImageDao.findMostRecentDateModifiedOccurrence().toDomain()


    override suspend fun insert(model: ParticipantImageFile, orReplace: Boolean) = transactionRunner.withTransaction {
        try {
            val entity = model.toPersistence()
            val insertedImage = (if (orReplace) participantImageDao.insertOrReplace(entity) else participantImageDao.insert(entity)) > 0
            if (!insertedImage) {
                throw InsertEntityException("Cannot save participant image: ${model.participantUuid}", orReplace = orReplace)
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save participant image", orReplace = orReplace)
        }
    }

    override suspend fun deleteAll() {
        participantImageDao.deleteAll()
    }

    override fun observeChanges(): Flow<Long> {
        return participantImageDao.observeChanges()
    }

}