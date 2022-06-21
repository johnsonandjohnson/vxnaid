package com.jnj.vaccinetracker.common.data.database.daos.draft

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftParticipantDataFileDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftParticipantSyncDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ImageDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantImageEntity
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateModel
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftParticipantImageDao : ImageDaoBase<DraftParticipantImageEntity>, DraftParticipantDataFileDaoBase<DraftParticipantImageEntity>, ObservableDao,
    DraftParticipantSyncDao {

    @Query("select * from draft_participant_image where participantUuid=:participantUuid")
    suspend fun findByParticipantUuid(participantUuid: String): DraftParticipantImageEntity?

    @Query("select count(*) from draft_participant_image")
    override fun observeChanges(): Flow<Long>

    @Update(entity = DraftParticipantImageEntity::class)
    override suspend fun updateDraftState(updateDraftStateModel: RoomUpdateParticipantDraftStateModel): Int

    @Query("delete from draft_participant_image where draftState=:draftState")
    override suspend fun deleteAllByDraftState(draftState: DraftState): Int

    @Query("select * from draft_participant_image where draftState=:draftState LIMIT :offset,:limit")
    override suspend fun findAllByDraftState(draftState: DraftState, offset: Int, limit: Int): List<DraftParticipantImageEntity>

    @Query("select count(*) from draft_participant_image where draftState=:draftState")
    suspend fun countByDraftState(draftState: DraftState): Long

    @Delete(entity = DraftParticipantImageEntity::class)
    override suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int


    @Query("select * from draft_participant_image LIMIT :offset, :limit")
    override suspend fun findAll(offset: Int, limit: Int): List<DraftParticipantImageEntity>

}