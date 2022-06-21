package com.jnj.vaccinetracker.common.data.database.daos.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftVisitDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftVisitSyncDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterObservationEntity
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteVisitModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantDataToUploadModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftVisitEncounterModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateVisitDraftStateModel
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftVisitEncounterDao : DraftVisitDaoBase<DraftVisitEncounterEntity, RoomDraftVisitEncounterModel>, ObservableDao, DraftVisitSyncDao {

    @Query("DELETE FROM draft_visit_encounter where visitUuid=:visitUuid and draftState=:draftState")
    override suspend fun deleteByVisitUuid(visitUuid: String, draftState: DraftState): Int

    @Query("select * from draft_visit_encounter where participantUuid=:participantUuid AND draftState=:draftState")
    @Transaction
    override suspend fun findAllByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): List<RoomDraftVisitEncounterModel>

    @Query("select * from draft_visit_encounter where participantUuid=:participantUuid")
    @Transaction
    override suspend fun findAllByParticipantUuid(participantUuid: String): List<RoomDraftVisitEncounterModel>

    @Query("select * from draft_visit_encounter where visitUuid=:visitUuid")
    @Transaction
    override suspend fun findByVisitUuid(visitUuid: String): RoomDraftVisitEncounterModel?

    @Query("select * from draft_visit_encounter where draftState=:draftState LIMIT 1")
    @Transaction
    suspend fun findFirstByDraftState(draftState: DraftState): RoomDraftVisitEncounterModel?

    @Update(entity = DraftVisitEncounterEntity::class)
    override suspend fun updateDraftState(updateDraftStateModel: RoomUpdateVisitDraftStateModel): Int

    @Query("select count(*) from draft_visit_encounter")
    override fun observeChanges(): Flow<Long>

    @Query("select participantUuid from draft_visit_encounter where draftState=:draftState LIMIT :offset,:limit")
    override suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<RoomDraftParticipantDataToUploadModel>

    @Query("delete from draft_visit_encounter where draftState=:draftState")
    override suspend fun deleteAllByDraftState(draftState: DraftState): Int

    @Query("select draftState from draft_visit_encounter where visitUuid=:visitUuid")
    override suspend fun findDraftStateByVisitUuid(visitUuid: String): DraftState?

    @Query("select count(*) from draft_visit_encounter where draftState=:draftState")
    suspend fun countByDraftState(draftState: DraftState): Long

    @Delete(entity = DraftVisitEncounterEntity::class)
    override suspend fun delete(deleteVisitModel: RoomDeleteVisitModel): Int
}

@Dao
interface DraftVisitEncounterAttributeDao : DaoBase<DraftVisitEncounterAttributeEntity>, ObservableDao {

    @Query("select count(*) from draft_visit_encounter_attribute")
    override fun observeChanges(): Flow<Long>
}

@Dao
interface DraftVisitEncounterObservationDao : DaoBase<DraftVisitEncounterObservationEntity>, ObservableDao {

    @Query("select count(*) from draft_visit_encounter_observation")
    override fun observeChanges(): Flow<Long>
}

