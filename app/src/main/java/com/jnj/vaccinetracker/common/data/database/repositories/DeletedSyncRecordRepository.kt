package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.DeletedBiometricsTemplateDao
import com.jnj.vaccinetracker.common.data.database.daos.DeletedImageDao
import com.jnj.vaccinetracker.common.data.database.daos.DeletedParticipantDao
import com.jnj.vaccinetracker.common.data.database.daos.DeletedVisitDao
import com.jnj.vaccinetracker.common.data.database.daos.base.insert
import com.jnj.vaccinetracker.common.data.database.entities.*
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel.Companion.toDomain
import com.jnj.vaccinetracker.common.data.database.models.RoomDeletedParticipantModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import javax.inject.Inject

class DeletedSyncRecordRepository @Inject constructor(
    private val deletedVisitDao: DeletedVisitDao,
    private val deletedImageDao: DeletedImageDao,
    private val deletedBiometricsTemplateDao: DeletedBiometricsTemplateDao,
    private val deletedParticipantDao: DeletedParticipantDao,
) {

    private fun SyncEntityType.dao() = when (this) {
        SyncEntityType.PARTICIPANT -> deletedParticipantDao
        SyncEntityType.IMAGE -> deletedImageDao
        SyncEntityType.BIOMETRICS_TEMPLATE -> deletedBiometricsTemplateDao
        SyncEntityType.VISIT -> deletedVisitDao
    }

    private fun DeletedSyncRecord.BiometricsTemplate.toPersistence() =
        DeletedParticipantBiometricsTemplateEntity(participantUuid, dateModified.date)

    private fun DeletedSyncRecord.Image.toPersistence() = DeletedParticipantImageEntity(participantUuid, dateModified.date)
    private fun DeletedSyncRecord.Participant.toPersistence() = DeletedParticipantEntity(participantUuid = participantUuid,
        dateModified = dateModified.date, participantId = participantId )

    private fun DeletedSyncRecord.Visit.toPersistence() = DeletedVisitEntity(visitUuid = visitUuid, participantUuid = participantUuid, dateModified = dateModified.date)

    private fun DateEntity.toSyncDate() = SyncDate(time)

    private fun DeletedSyncRecordEntityBase.toDomain(): DeletedSyncRecord = when (this) {
        is DeletedParticipantBiometricsTemplateEntity -> DeletedSyncRecord.BiometricsTemplate(participantUuid, dateModified.toSyncDate())
        is DeletedParticipantImageEntity -> DeletedSyncRecord.Image(participantUuid, dateModified.toSyncDate())
        is DeletedParticipantEntity -> DeletedSyncRecord.Participant(participantUuid = participantUuid,
            dateModified = dateModified.toSyncDate(),participantId = participantId )
        is DeletedVisitEntity -> DeletedSyncRecord.Visit(visitUuid, participantUuid, dateModified.toSyncDate())
    }

    suspend fun insert(model: DeletedSyncRecord, orReplace: Boolean) {
        try {
            val id = when (model) {
                is DeletedSyncRecord.BiometricsTemplate -> deletedBiometricsTemplateDao.insert(model.toPersistence(), orReplace)
                is DeletedSyncRecord.Image -> deletedImageDao.insert(model.toPersistence(), orReplace)
                is DeletedSyncRecord.Participant -> deletedParticipantDao.insert(model.toPersistence(), orReplace)
                is DeletedSyncRecord.Visit -> deletedVisitDao.insert(model.toPersistence(), orReplace)
            }
            if (id == 0L)
                throw InsertEntityException("cannot save deleted sync record ${model.syncEntityType} ${model.uuid}", orReplace = false)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            if (ex is InsertEntityException)
                throw ex
            else
                throw InsertEntityException(cause = ex, message = "Something went wrong during insert deleted record", orReplace = orReplace)
        }
    }

    suspend fun delete(model: DeletedSyncRecord) {
        val countDeleted = when (model) {
            is DeletedSyncRecord.BiometricsTemplate -> deletedBiometricsTemplateDao.delete(model.toPersistence())
            is DeletedSyncRecord.Image -> deletedImageDao.delete(model.toPersistence())
            is DeletedSyncRecord.Participant -> deletedParticipantDao.delete(model.toPersistence())
            is DeletedSyncRecord.Visit -> deletedVisitDao.delete(model.toPersistence())
        }
        logInfo("delete:${model.syncEntityType} $countDeleted")
    }

    fun observeChanges(syncEntityType: SyncEntityType) = syncEntityType.dao().observeChanges()
    suspend fun count(syncEntityType: SyncEntityType): Long = observeChanges(syncEntityType).first()

    suspend fun deleteAll(syncEntityType: SyncEntityType) = syncEntityType.dao().deleteAll()

    suspend fun findMostRecentDateModifiedOccurrence(syncEntityType: SyncEntityType): DateModifiedOccurrence? =
        syncEntityType.dao().findMostRecentDateModifiedOccurrence().toDomain()

    suspend fun findByParticipantId(participantId: String): RoomDeletedParticipantModel? =
         deletedParticipantDao.findByParticipantId(participantId)
}