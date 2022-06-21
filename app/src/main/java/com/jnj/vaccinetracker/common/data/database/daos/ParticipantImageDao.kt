package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.ImageDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.daos.base.SyncDao
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantImageEntity
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantImageDao : ImageDaoBase<ParticipantImageEntity>, SyncDao, ObservableDao {

    @Query("select * from participant_image where participantUuid=:participantUuid")
    suspend fun findByParticipantUuid(participantUuid: String): ParticipantImageEntity?

    @Query("select participantUuid as uuid, dateModified from participant_image where dateModified = (select max(dateModified) from participant_image)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select count(*) from participant_image")
    override fun observeChanges(): Flow<Long>

    @Delete(entity = ParticipantImageEntity::class)
    suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int

    @Query("delete from participant_image")
    override suspend fun deleteAll()

    @Query("select * from participant_image LIMIT :offset, :limit")
    override suspend fun findAll(offset: Int, limit: Int): List<ParticipantImageEntity>
}