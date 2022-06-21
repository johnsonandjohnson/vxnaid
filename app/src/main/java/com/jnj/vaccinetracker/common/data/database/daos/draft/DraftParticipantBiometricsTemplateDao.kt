package com.jnj.vaccinetracker.common.data.database.daos.draft

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftParticipantDataFileDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftParticipantSyncDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ParticipantBiometricsTemplateDaoBase
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftParticipantBiometricsEntity
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateWithDateModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.exceptions.UpdateDraftStateException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield

@Dao
interface DraftParticipantBiometricsTemplateDao : ParticipantBiometricsTemplateDaoBase<DraftParticipantBiometricsEntity>, ObservableDao, DraftParticipantSyncDao,
    DraftParticipantDataFileDaoBase<DraftParticipantBiometricsEntity> {


    @Query("select * from draft_participant_biometrics_template LIMIT :offset, :limit")
    override suspend fun findAll(offset: Int, limit: Int): List<DraftParticipantBiometricsEntity>

    @Query("select * from draft_participant_biometrics_template where participantUuid=:participantUuid")
    override suspend fun findByParticipantUuid(participantUuid: String): DraftParticipantBiometricsEntity?

    @Query("""select draft_participant_biometrics_template.* from draft_participant_biometrics_template left join draft_participant 
        as dp USING (participantUuid) left join participant as p USING (participantUuid) where coalesce(dp.phone, p.phone) = :phone""")
    override suspend fun findAllByPhone(phone: String): List<DraftParticipantBiometricsEntity>

    @Query("""select draft_participant_biometrics_template.* from draft_participant_biometrics_template left join draft_participant 
        as dp USING (participantUuid) left join participant as p USING (participantUuid) where coalesce(dp.phone, p.phone) IS NULL LIMIT :offset, :limit""")
    override suspend fun findAllByPhoneIsNull(offset: Int, limit: Int): List<DraftParticipantBiometricsEntity>

    @Query("""select draft_participant_biometrics_template.* from draft_participant_biometrics_template left join draft_participant 
        as dp USING (participantUuid) left join participant as p USING (participantUuid) where coalesce(dp.participantId, p.participantId) = :participantId""")
    override suspend fun findByParticipantId(participantId: String): DraftParticipantBiometricsEntity?

    @Query("select count(*) from draft_participant_biometrics_template")
    override fun observeChanges(): Flow<Long>

    @Query("delete from draft_participant_biometrics_template where draftState=:draftState")
    override suspend fun deleteAllByDraftState(draftState: DraftState): Int

    @Update(entity = DraftParticipantBiometricsEntity::class)
    override suspend fun updateDraftState(updateDraftStateModel: RoomUpdateParticipantDraftStateModel): Int

    @Update(entity = DraftParticipantBiometricsEntity::class)
    suspend fun updateDraftStateWithDate(updateDraftStateModel: RoomUpdateParticipantDraftStateWithDateModel): Int

    @Query("""select * from draft_participant_biometrics_template where draftState=:draftState LIMIT :offset,:limit""")
    override suspend fun findAllByDraftState(draftState: DraftState, offset: Int, limit: Int): List<DraftParticipantBiometricsEntity>

    @Query("select count(*) from draft_participant_biometrics_template where draftState=:draftState")
    suspend fun countByDraftState(draftState: DraftState): Long

    @Query("select * from draft_participant_biometrics_template where draftState = :draftState AND ifnull(dateLastUploadAttempt, 0) < :date LIMIT :offset, :limit")
    suspend fun findAllByDateLastUploadAttemptLesserThan(date: DateEntity, draftState: DraftState, offset: Int, limit: Int): List<DraftParticipantBiometricsEntity>

    @Delete(entity = DraftParticipantBiometricsEntity::class)
    override suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int
}

suspend fun <T> DraftParticipantBiometricsTemplateDao.updateDraftStateWithDateOrThrow(updateDraftStateModel: RoomUpdateParticipantDraftStateWithDateModel) {
    logDebug("updateDraftStateWithDateOrThrow: $updateDraftStateModel")
    fun createError(throwable: Throwable?) = UpdateDraftStateException(
        cause = throwable,
        message = "Failed to update draft state with date [$updateDraftStateModel]"
    )

    val success = try {
        updateDraftStateWithDate(updateDraftStateModel) > 0
    } catch (throwable: Throwable) {
        yield()
        throwable.rethrowIfFatal()
        throw createError(throwable)
    }
    if (!success) {
        throw createError(null)
    }
}