package com.jnj.vaccinetracker.common.data.database.daos.base

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface DaoBase<in E> {

    @Update
    suspend fun update(entity: E): Int

    @Update
    suspend fun updateAll(entities: Collection<E>): Int

    @Delete
    suspend fun delete(entity: E): Int

    @Delete
    suspend fun deleteAll(entities: Collection<E>): Int

    @Insert
    suspend fun insert(entity: E): Long

    @Insert
    suspend fun insertAll(entities: Collection<E>): List<@JvmSuppressWildcards Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: E): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(entities: Collection<E>): List<@JvmSuppressWildcards Long>
}

suspend fun <E> DaoBase<E>.insert(entity: E, orReplace: Boolean): Long = if (orReplace) insertOrReplace(entity) else insert(entity)

interface ObservableDao {

    /**
     * We don't care about the return type but Room requires it be non generic.
     *
     * Just write the query like:
     * ```
     * select count(*) from $tableName
     * ```
     *
     * Each time the table is updated, this will be triggered
     */
    fun observeChanges(): Flow<Long>
}

suspend fun ObservableDao.count(): Long = observeChanges().first()
