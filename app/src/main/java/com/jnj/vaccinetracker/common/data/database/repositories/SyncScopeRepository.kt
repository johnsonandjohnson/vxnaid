package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.SyncScopeDao
import com.jnj.vaccinetracker.common.data.database.entities.SyncScopeEntity
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.exceptions.SyncScopeEntityNotFoundException
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import javax.inject.Inject

class SyncScopeRepository @Inject constructor(
    private val syncScopeDao: SyncScopeDao,
    private val transactionRunner: ParticipantDbTransactionRunner,
) {

    private fun SyncScope.toPersistence() = SyncScopeEntity(siteUuid = siteUuid, level = level, dateCreated = dateCreated, country = country, cluster = cluster)
    private fun SyncScopeEntity.toDomain() = SyncScope(siteUuid = siteUuid, level = level, dateCreated = dateCreated, country = country, cluster = cluster)

    suspend fun findOne(): SyncScope? {
        return syncScopeDao.findOne()?.toDomain()
    }

    suspend fun insert(syncScope: SyncScope) {
        val entity = syncScope.toPersistence()
        val success = syncScopeDao.insert(entity) > 0L
        if (!success) {
            throw InsertEntityException("could not insert syncScope", orReplace = false)
        }
    }

    /**
     * throws [SyncScopeEntityNotFoundException] when there's no existing sync scope to delete
     */
    suspend fun deleteExisting(): Int = transactionRunner.withTransaction {
        val syncScope = syncScopeDao.findOne() ?: throw SyncScopeEntityNotFoundException()
        syncScopeDao.delete(syncScope)
    }
}