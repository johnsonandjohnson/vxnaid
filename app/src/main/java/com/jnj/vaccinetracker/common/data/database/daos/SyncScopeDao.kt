package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.SyncScopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncScopeDao : DaoBase<SyncScopeEntity>, ObservableDao {

    @Query("select * from sync_scope where id = 1")
    suspend fun findOne(): SyncScopeEntity?

    @Query("select count(*) from sync_scope")
    override fun observeChanges(): Flow<Long>
}