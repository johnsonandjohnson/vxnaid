package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.*
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel
import com.jnj.vaccinetracker.common.data.database.models.RoomDeletedParticipantModel
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import kotlinx.coroutines.flow.Flow

interface DeletedSyncRecordDao<T : DeletedSyncRecordEntityBase> : DaoBase<T>, ObservableDao {
    suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>
    suspend fun deleteAll()
}

@Dao
interface DeletedParticipantDao : DeletedSyncRecordDao<DeletedParticipantEntity> {

    @Query("DELETE from deleted_participant where participantUuid=:participantUuid")
    fun deleteByParticipantUuid(participantUuid: String): Int

    @Query("select participantUuid as uuid, dateModified from deleted_participant where dateModified = (select max(dateModified) from deleted_participant)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select count(*) from deleted_participant")
    override fun observeChanges(): Flow<Long>

    @Query("delete from deleted_participant")
    override suspend fun deleteAll()

    @Query("select participantUuid as uuid, dateModified, participantId  from deleted_participant where participantId=:participantId")
    suspend fun findByParticipantId(participantId: String) :RoomDeletedParticipantModel?
}

@Dao
interface DeletedImageDao : DeletedSyncRecordDao<DeletedParticipantImageEntity> {
    @Query("DELETE from deleted_participant_image where participantUuid=:participantUuid")
    fun deleteByParticipantUuid(participantUuid: String): Int

    @Query("select participantUuid as uuid, dateModified from deleted_participant_image where dateModified = (select max(dateModified) from deleted_participant_image)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>


    @Query("select count(*) from deleted_participant_image")
    override fun observeChanges(): Flow<Long>

    @Query("delete from deleted_participant_image")
    override suspend fun deleteAll()

}

@Dao
interface DeletedBiometricsTemplateDao : DeletedSyncRecordDao<DeletedParticipantBiometricsTemplateEntity> {
    @Query("DELETE from deleted_participant_biometrics_template where participantUuid=:participantUuid")
    fun deleteByParticipantUuid(participantUuid: String): Int

    @Query("select participantUuid as uuid, dateModified from deleted_participant_biometrics_template where dateModified = (select max(dateModified) from deleted_participant_biometrics_template)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select count(*) from deleted_participant_biometrics_template")
    override fun observeChanges(): Flow<Long>


    @Query("delete from deleted_participant_biometrics_template")
    override suspend fun deleteAll()
}

@Dao
interface DeletedVisitDao : DeletedSyncRecordDao<DeletedVisitEntity> {
    @Query("select count(*) from deleted_visit")
    override fun observeChanges(): Flow<Long>

    @Query("select visitUuid as uuid, dateModified from deleted_visit where dateModified = (select max(dateModified) from deleted_visit)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("DELETE from deleted_visit where visitUuid=:visitUuid")
    fun deleteByVisitUuid(visitUuid: String): Int

    @Query("delete from deleted_visit")
    override suspend fun deleteAll()
}
