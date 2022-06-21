package com.jnj.vaccinetracker.common.data.database.daos.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftParticipantDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftParticipantSyncDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantAddressEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantEntity
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantDataToUploadModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateModel
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftParticipantDao : DraftParticipantDaoBase<DraftParticipantEntity, RoomDraftParticipantModel>, ObservableDao, DraftParticipantSyncDao {

    @Query("select * from draft_participant left join draft_participant_address using (participantUuid) where phone=:phone")
    @Transaction
    override suspend fun findAllByPhone(phone: String): List<RoomDraftParticipantModel>

    @Query("select * from draft_participant left join draft_participant_address using (participantUuid) where phone IS NULL")
    @Transaction
    override suspend fun findAllByPhoneIsNull(): List<RoomDraftParticipantModel>

    @Query("select * from draft_participant left join draft_participant_address using (participantUuid) where participantUuid=:participantUuid")
    @Transaction
    override suspend fun findByParticipantUuid(participantUuid: String): RoomDraftParticipantModel?

    @Query("select * from draft_participant left join draft_participant_address using (participantUuid) where participantUuid=:participantUuid AND  draftState=:draftState")
    @Transaction
    override suspend fun findByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): RoomDraftParticipantModel?

    @Query("select draftState from draft_participant where participantUuid=:participantUuid")
    suspend fun findDraftStateByParticipantUuid(participantUuid: String): DraftState?

    @Query("select * from draft_participant left join draft_participant_address using (participantUuid) where participantId=:participantId")
    @Transaction
    override suspend fun findByParticipantId(participantId: String): RoomDraftParticipantModel?

    @Query("select * from draft_participant left join draft_participant_address using (participantUuid) where draftState=:draftState LIMIT 1")
    @Transaction
    suspend fun findFirstByDraftState(draftState: DraftState): RoomDraftParticipantModel?

    @Query("select count(*) from draft_participant")
    override fun observeChanges(): Flow<Long>

    @Query("select count(*) from draft_participant where draftState=:draftState")
    suspend fun countByDraftState(draftState: DraftState): Long

    @Query("delete from draft_participant where draftState=:draftState")
    override suspend fun deleteAllByDraftState(draftState: DraftState): Int

    @Update(entity = DraftParticipantEntity::class)
    override suspend fun updateDraftState(updateDraftStateModel: RoomUpdateParticipantDraftStateModel): Int

    @Query("select participantUuid from draft_participant where draftState=:draftState LIMIT :offset, :limit")
    override suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<RoomDraftParticipantDataToUploadModel>

    @Delete(entity = DraftParticipantEntity::class)
    override suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int
}

@Dao
interface DraftParticipantAttributeDao : DaoBase<DraftParticipantAttributeEntity>, ObservableDao {


    @Query("select value from draft_participant_attribute where participantUuid=:participantUuid AND type = :type")
    suspend fun findAttribute(participantUuid: String, type: String): String?

    @Query("select count(*) from draft_participant_attribute")
    override fun observeChanges(): Flow<Long>
}

@Dao
interface DraftParticipantAddressDao : DaoBase<DraftParticipantAddressEntity>, ObservableDao {

    @Query("select count(*) from draft_participant_address")
    override fun observeChanges(): Flow<Long>
}