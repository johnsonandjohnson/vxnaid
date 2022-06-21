package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.OperatorCredentialsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperatorCredentialsDao : DaoBase<OperatorCredentialsEntity>, ObservableDao {
    @Query("select * from operator")
    suspend fun findAll(): List<OperatorCredentialsEntity>

    @Query("select * from operator where username = :username")
    suspend fun findByUsername(username: String): OperatorCredentialsEntity?

    @Query("select count(*) from operator")
    override fun observeChanges(): Flow<Long>
}