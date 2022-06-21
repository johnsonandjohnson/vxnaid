package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.FailedBiometricsTemplateSyncRecordDownloadDao
import com.jnj.vaccinetracker.common.data.database.daos.FailedImageSyncRecordDownloadDao
import com.jnj.vaccinetracker.common.data.database.daos.FailedParticipantSyncRecordDownloadDao
import com.jnj.vaccinetracker.common.data.database.daos.FailedVisitSyncRecordDownloadDao
import com.jnj.vaccinetracker.common.data.database.daos.base.insert
import com.jnj.vaccinetracker.common.data.database.entities.*
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel.Companion.toDomain
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import javax.inject.Inject

class FailedSyncRecordDownloadRepository @Inject constructor(
    private val failedVisitSyncRecordDownloadDao: FailedVisitSyncRecordDownloadDao,
    private val failedImageSyncRecordDownloadDao: FailedImageSyncRecordDownloadDao,
    private val failedBiometricsTemplateSyncRecordDownloadDao: FailedBiometricsTemplateSyncRecordDownloadDao,
    private val failedParticipantSyncRecordDownloadDao: FailedParticipantSyncRecordDownloadDao,
) {

    private fun SyncEntityType.dao() = when (this) {
        SyncEntityType.PARTICIPANT -> failedParticipantSyncRecordDownloadDao
        SyncEntityType.IMAGE -> failedImageSyncRecordDownloadDao
        SyncEntityType.BIOMETRICS_TEMPLATE -> failedBiometricsTemplateSyncRecordDownloadDao
        SyncEntityType.VISIT -> failedVisitSyncRecordDownloadDao
    }

    private fun FailedSyncRecordDownload.BiometricsTemplate.toPersistence() =
        FailedBiometricsTemplateSyncRecordDownloadEntity(participantUuid, dateLastDownloadAttempt, dateModified.date)

    private fun FailedSyncRecordDownload.Image.toPersistence() = FailedImageSyncRecordDownloadEntity(participantUuid, dateLastDownloadAttempt, dateModified.date)
    private fun FailedSyncRecordDownload.Participant.toPersistence() = FailedParticipantSyncRecordDownloadEntity(participantUuid = participantUuid,
        dateLastDownloadAttempt = dateLastDownloadAttempt,
        dateModified = dateModified.date)

    private fun FailedSyncRecordDownload.Visit.toPersistence() = FailedVisitSyncRecordDownloadEntity(visitUuid, participantUuid, dateLastDownloadAttempt, dateModified.date)

    private fun DateEntity.toSyncDate() = SyncDate(time)
    private fun FailedSyncRecordDownloadEntityBase.toDomain(): FailedSyncRecordDownload = when (this) {
        is FailedBiometricsTemplateSyncRecordDownloadEntity -> FailedSyncRecordDownload.BiometricsTemplate(participantUuid, dateModified.toSyncDate(), dateLastDownloadAttempt)
        is FailedImageSyncRecordDownloadEntity -> FailedSyncRecordDownload.Image(participantUuid, dateModified.toSyncDate(), dateLastDownloadAttempt)
        is FailedParticipantSyncRecordDownloadEntity -> FailedSyncRecordDownload.Participant(participantUuid = participantUuid,
            dateModified = dateModified.toSyncDate(),
            dateLastDownloadAttempt = dateLastDownloadAttempt)
        is FailedVisitSyncRecordDownloadEntity -> FailedSyncRecordDownload.Visit(visitUuid, participantUuid, dateModified.toSyncDate(), dateLastDownloadAttempt)
    }

    suspend fun insert(model: FailedSyncRecordDownload, orReplace: Boolean) {
        try {
            val id = when (model) {
                is FailedSyncRecordDownload.BiometricsTemplate -> failedBiometricsTemplateSyncRecordDownloadDao.insert(model.toPersistence(), orReplace)
                is FailedSyncRecordDownload.Image -> failedImageSyncRecordDownloadDao.insert(model.toPersistence(), orReplace)
                is FailedSyncRecordDownload.Participant -> failedParticipantSyncRecordDownloadDao.insert(model.toPersistence(), orReplace)
                is FailedSyncRecordDownload.Visit -> failedVisitSyncRecordDownloadDao.insert(model.toPersistence(), orReplace)
            }
            if (id == 0L)
                throw InsertEntityException("cannot save failed sync record ${model.syncEntityType} ${model.uuid}", orReplace = false)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            if (ex is InsertEntityException)
                throw ex
            else
                throw InsertEntityException(cause = ex, message = "Something went wrong during insert failed record", orReplace = orReplace)
        }
    }

    suspend fun delete(model: FailedSyncRecordDownload) {
        val countDeleted = when (model) {
            is FailedSyncRecordDownload.BiometricsTemplate -> failedBiometricsTemplateSyncRecordDownloadDao.delete(model.toPersistence())
            is FailedSyncRecordDownload.Image -> failedImageSyncRecordDownloadDao.delete(model.toPersistence())
            is FailedSyncRecordDownload.Participant -> failedParticipantSyncRecordDownloadDao.delete(model.toPersistence())
            is FailedSyncRecordDownload.Visit -> failedVisitSyncRecordDownloadDao.delete(model.toPersistence())
        }
        logDebug("delete:${model.syncEntityType} $countDeleted")
    }

    fun observeChanges(syncEntityType: SyncEntityType) = syncEntityType.dao().observeChanges()
    suspend fun count(syncEntityType: SyncEntityType): Long = observeChanges(syncEntityType).first()

    suspend fun findAllByDateLastDownloadAttemptLesserThan(syncEntityType: SyncEntityType, date: DateEntity, offset: Int, limit: Int): List<FailedSyncRecordDownload> {
        return syncEntityType.dao().findAllByDateLastDownloadAttemptLesserThan(date, offset, limit).map { it.toDomain() }
    }

    suspend fun deleteAll(syncEntityType: SyncEntityType) = syncEntityType.dao().deleteAll()

    suspend fun findAllParticipantUuids(syncEntityType: SyncEntityType): List<String> {
        return syncEntityType.dao().findAllParticipantUuids()
    }

    suspend fun findMostRecentDateModifiedOccurrence(syncEntityType: SyncEntityType): DateModifiedOccurrence? =
        syncEntityType.dao().findMostRecentDateModifiedOccurrence().toDomain()

}