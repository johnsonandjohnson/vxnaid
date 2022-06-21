package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.*
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import kotlinx.coroutines.flow.Flow

interface FailedSyncRecordDownloadDao<T : FailedSyncRecordDownloadEntityBase> : DaoBase<T>, ObservableDao {
    suspend fun findAllByDateLastDownloadAttemptLesserThan(date: DateEntity, offset: Int, limit: Int): List<@JvmSuppressWildcards T>
    suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>
    suspend fun deleteAll()
    suspend fun findAllParticipantUuids(): List<String>
}

@Dao
interface FailedParticipantSyncRecordDownloadDao : FailedSyncRecordDownloadDao<FailedParticipantSyncRecordDownloadEntity> {

    @Query("DELETE from failed_participant_download where participantUuid=:participantUuid")
    fun deleteByParticipantUuid(participantUuid: String): Int

    @Query("select participantUuid as uuid, dateModified from failed_participant_download where dateModified = (select max(dateModified) from failed_participant_download)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select * from failed_participant_download where dateLastDownloadAttempt < :date LIMIT :offset, :limit")
    override suspend fun findAllByDateLastDownloadAttemptLesserThan(date: DateEntity, offset: Int, limit: Int): List<FailedParticipantSyncRecordDownloadEntity>

    @Query("select count(*) from failed_participant_download")
    override fun observeChanges(): Flow<Long>

    @Query("delete from failed_participant_download")
    override suspend fun deleteAll()

    @Query("select participantUuid from failed_participant_download")
    override suspend fun findAllParticipantUuids(): List<String>
}

@Dao
interface FailedImageSyncRecordDownloadDao : FailedSyncRecordDownloadDao<FailedImageSyncRecordDownloadEntity> {
    @Query("DELETE from failed_image_download where participantUuid=:participantUuid")
    fun deleteByParticipantUuid(participantUuid: String): Int

    @Query("select participantUuid as uuid, dateModified from failed_image_download where dateModified = (select max(dateModified) from failed_image_download)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select * from failed_image_download where dateLastDownloadAttempt < :date LIMIT :offset, :limit")
    override suspend fun findAllByDateLastDownloadAttemptLesserThan(date: DateEntity, offset: Int, limit: Int): List<FailedImageSyncRecordDownloadEntity>

    @Query("select count(*) from failed_image_download")
    override fun observeChanges(): Flow<Long>

    @Query("delete from failed_image_download")
    override suspend fun deleteAll()

    @Query("select participantUuid from failed_image_download")
    override suspend fun findAllParticipantUuids(): List<String>
}

@Dao
interface FailedBiometricsTemplateSyncRecordDownloadDao : FailedSyncRecordDownloadDao<FailedBiometricsTemplateSyncRecordDownloadEntity> {
    @Query("DELETE from failed_biometrics_template_download where participantUuid=:participantUuid")
    fun deleteByParticipantUuid(participantUuid: String): Int

    @Query("select participantUuid as uuid, dateModified from failed_biometrics_template_download where dateModified = (select max(dateModified) from failed_biometrics_template_download)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select count(*) from failed_biometrics_template_download")
    override fun observeChanges(): Flow<Long>

    @Query("select * from failed_biometrics_template_download where dateLastDownloadAttempt < :date LIMIT :offset, :limit")
    override suspend fun findAllByDateLastDownloadAttemptLesserThan(date: DateEntity, offset: Int, limit: Int): List<FailedBiometricsTemplateSyncRecordDownloadEntity>

    @Query("delete from failed_biometrics_template_download")
    override suspend fun deleteAll()

    @Query("select participantUuid from failed_biometrics_template_download")
    override suspend fun findAllParticipantUuids(): List<String>
}

@Dao
interface FailedVisitSyncRecordDownloadDao : FailedSyncRecordDownloadDao<FailedVisitSyncRecordDownloadEntity> {
    @Query("select count(*) from failed_visit_download")
    override fun observeChanges(): Flow<Long>

    @Query("select visitUuid as uuid, dateModified from failed_visit_download where dateModified = (select max(dateModified) from failed_visit_download)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("DELETE from failed_visit_download where visitUuid=:visitUuid")
    fun deleteByVisitUuid(visitUuid: String): Int

    @Query("select * from failed_visit_download where dateLastDownloadAttempt < :date LIMIT :offset, :limit")
    override suspend fun findAllByDateLastDownloadAttemptLesserThan(date: DateEntity, offset: Int, limit: Int): List<FailedVisitSyncRecordDownloadEntity>

    @Query("delete from failed_visit_download")
    override suspend fun deleteAll()

    @Query("select participantUuid from failed_visit_download")
    override suspend fun findAllParticipantUuids(): List<String>
}
