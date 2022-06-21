package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ParticipantBiometricsTemplateDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.SyncDao
import com.jnj.vaccinetracker.common.data.database.entities.ParticipantBiometricsEntity
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantBiometricsTemplateDao : ParticipantBiometricsTemplateDaoBase<ParticipantBiometricsEntity>, SyncDao, ObservableDao {

    @Query("select participant_biometrics_template.* from participant_biometrics_template join participant USING (participantUuid) where phone=:phone")
    override suspend fun findAllByPhone(phone: String): List<ParticipantBiometricsEntity>

    @Query("select * from participant_biometrics_template LIMIT :offset, :limit")
    override suspend fun findAll(offset: Int, limit: Int): List<ParticipantBiometricsEntity>

    @Query("select participant_biometrics_template.* from participant_biometrics_template join participant USING (participantUuid) where phone IS NULL limit :offset, :limit")
    override suspend fun findAllByPhoneIsNull(offset: Int, limit: Int): List<ParticipantBiometricsEntity>

    @Query("select participant_biometrics_template.* from participant_biometrics_template join participant USING (participantUuid) where participantId=:participantId")
    override suspend fun findByParticipantId(participantId: String): ParticipantBiometricsEntity?

    @Query("select * from participant_biometrics_template where participantUuid=:participantUuid")
    override suspend fun findByParticipantUuid(participantUuid: String): ParticipantBiometricsEntity?

    @Query("select participantUuid as uuid, dateModified from participant_biometrics_template where dateModified = (select max(dateModified) from participant_biometrics_template)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Delete(entity = ParticipantBiometricsEntity::class)
    override suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int

    @Query("select count(*) from participant_biometrics_template")
    override fun observeChanges(): Flow<Long>

    @Query("delete from participant_biometrics_template")
    override suspend fun deleteAll()
}