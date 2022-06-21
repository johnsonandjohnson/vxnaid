package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.SyncErrorEntity
import com.jnj.vaccinetracker.common.data.database.models.syncerror.RoomSyncErrorOverviewModel
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncErrorDao : DaoBase<SyncErrorEntity>, ObservableDao {
    @Query("select * from sync_error where syncErrorState in (:syncErrorStates) LIMIT :offset, :limit")
    suspend fun findAllByErrorStates(syncErrorStates: List<SyncErrorState>, offset: Int, limit: Int): List<SyncErrorEntity>

    /**
     * note: [syncErrorState] can only be incremented.
     */
    @Query("UPDATE sync_error SET syncErrorState=:syncErrorState where syncErrorState<:syncErrorState and id in (:ids)")
    suspend fun updateAllErrorState(syncErrorState: SyncErrorState, ids: List<String>): Int

    @Query("select metadataJson from sync_error LIMIT :offset, :limit")
    suspend fun findAllMetadata(offset: Int, limit: Int): List<String>

    @Query("select metadataJson, dateCreated from sync_error where syncErrorState in (:syncErrorStates) order by dateCreated desc LIMIT :offset, :limit")
    suspend fun findAllOverview(syncErrorStates: List<SyncErrorState>, offset: Int, limit: Int): List<RoomSyncErrorOverviewModel>

    @Query("select * from sync_error where id = :id")
    suspend fun findById(id: String): SyncErrorEntity?

    @Query("select id from sync_error where type = :type and syncErrorState in (:syncErrorStates)")
    suspend fun findAllIdsByType(syncErrorStates: List<SyncErrorState>, type: String): List<String>

    @Query("select id from sync_error where syncErrorState = :syncErrorState LIMIT :limit")
    suspend fun findAllIdsByErrorState(syncErrorState: SyncErrorState, limit: Int): List<String>

    @Query("DELETE FROM sync_error")
    suspend fun deleteAll()

    @Query("DELETE FROM sync_error where syncErrorState = :syncErrorState and id in (:ids)")
    suspend fun deleteAllByIdsAndErrorState(ids: List<String>, syncErrorState: SyncErrorState): Int

    @Query("select count(*) from sync_error")
    override fun observeChanges(): Flow<Long>

    @Query("select count(*) from sync_error where syncErrorState in (:syncErrorStates)")
    suspend fun countByErrorStates(syncErrorStates: List<SyncErrorState>): Long
}