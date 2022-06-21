package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ParticipantDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.SyncDao
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantAddressEntity
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantEntity
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel
import com.jnj.vaccinetracker.common.data.database.models.RoomParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao : ParticipantDaoBase<ParticipantEntity, RoomParticipantModel>, ObservableDao, SyncDao {

    @Query("select * from participant left join participant_address using (participantUuid) where participantUuid=:participantUuid")
    @Transaction
    override suspend fun findByParticipantUuid(participantUuid: String): RoomParticipantModel?

    @Query("select * from participant left join participant_address using (participantUuid) where participantId=:participantId")
    @Transaction
    override suspend fun findByParticipantId(participantId: String): RoomParticipantModel?

    @Query("select * from participant left join participant_address using (participantUuid) where phone=:phone")
    @Transaction
    override suspend fun findAllByPhone(phone: String): List<RoomParticipantModel>

    @Query("select * from participant left join participant_address using (participantUuid) where phone IS NULL")
    @Transaction
    override suspend fun findAllByPhoneIsNull(): List<RoomParticipantModel>

    @Query("select participantUuid as uuid, dateModified from participant where dateModified = (select max(dateModified) from participant)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select count(*) from participant")
    override fun observeChanges(): Flow<Long>

    @Delete(entity = ParticipantEntity::class)
    override suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int

    @Query("delete from participant")
    override suspend fun deleteAll()
}


@Dao
interface ParticipantAttributeDao : DaoBase<ParticipantAttributeEntity>, ObservableDao {

    @Query("select value from participant_attribute where participantUuid=:participantUuid AND type = :type")
    suspend fun findAttribute(participantUuid: String, type: String): String?

    @Query("select count(*) from participant_attribute")
    override fun observeChanges(): Flow<Long>
}

@Dao
interface ParticipantAddressDao : DaoBase<ParticipantAddressEntity>, ObservableDao {

    @Query("select count(*) from participant_address")
    override fun observeChanges(): Flow<Long>
}