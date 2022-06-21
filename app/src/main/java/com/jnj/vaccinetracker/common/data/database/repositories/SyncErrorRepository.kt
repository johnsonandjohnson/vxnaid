package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.SyncErrorDao
import com.jnj.vaccinetracker.common.data.database.entities.SyncErrorEntity
import com.jnj.vaccinetracker.common.data.database.helpers.chunkedQueryByIds
import com.jnj.vaccinetracker.common.data.database.mappers.SyncErrorJsonMapper
import com.jnj.vaccinetracker.common.data.database.models.syncerror.RoomSyncErrorOverviewModel
import com.jnj.vaccinetracker.common.data.database.repositories.base.RepositoryBase
import com.jnj.vaccinetracker.common.exceptions.InsertEntityException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.domain.entities.SyncError
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorOverview
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncErrorRepository @Inject constructor(
    private val syncErrorDao: SyncErrorDao,
    private val syncErrorMetadataJsonMapper: SyncErrorJsonMapper,
) : RepositoryBase<SyncError> {

    companion object {
        /**
         * make small to save backend bandwidth
         */
        const val STACKTRACE_MAX_LENGTH = 2000
    }

    private fun String.truncateStackTrace(): String = take(STACKTRACE_MAX_LENGTH)

    private suspend fun SyncError.toPersistence() = SyncErrorEntity(
        id = key,
        metadataJson = syncErrorMetadataJsonMapper.toJson(metadata),
        stackTrace = stackTrace.truncateStackTrace(),
        dateCreated = dateCreated,
        type = metadata.type,
        syncErrorState = syncErrorState
    )

    private suspend fun SyncErrorEntity.toDomain() =
        SyncError(
            metadata = syncErrorMetadataJsonMapper.fromJson(metadataJson),
            stackTrace = stackTrace,
            dateCreated = dateCreated,
            syncErrorState = syncErrorState
        )


    private suspend fun RoomSyncErrorOverviewModel.toDomain() = SyncErrorOverview(syncErrorMetadataJsonMapper.fromJson(metadataJson), dateCreated)

    suspend fun findAllSyncErrorKeysByType(syncErrorStates: List<SyncErrorState>, type: String): List<String> {
        return syncErrorDao.findAllIdsByType(syncErrorStates, type)
    }

    suspend fun findAllOverview(syncErrorStates: List<SyncErrorState>, offset: Int, limit: Int): List<SyncErrorOverview> =
        syncErrorDao.findAllOverview(syncErrorStates, offset = offset, limit = limit).map { it.toDomain() }

    suspend fun findByKey(key: String): SyncError? = syncErrorDao.findById(key)?.toDomain()

    suspend fun deleteAll() {
        logInfo("deleteAll")
        syncErrorDao.deleteAll()
    }

    suspend fun deleteByKeysAndErrorState(keys: List<String>, errorState: SyncErrorState) {
        suspend fun statement(ids: List<String>) = syncErrorDao.deleteAllByIdsAndErrorState(ids, errorState)
        val countDeleted = chunkedQueryByIds(keys, ::statement).sum()
        val msg = "deleteAllByIdsAndErrorState ${keys.size} $errorState countDeleted:$countDeleted"
        if (keys.size != countDeleted)
            logWarn(msg)
        else
            logDebug(msg)
    }

    override suspend fun insert(model: SyncError, orReplace: Boolean) {
        val entity = model.toPersistence()
        try {
            val success = syncErrorDao.run {
                if (orReplace) insertOrReplace(entity) else insert(entity)
            } > 0L
            if (!success)
                throw InsertEntityException("Cannot save sync error: ${model.key}", orReplace = orReplace)
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            if (throwable is InsertEntityException)
                throw throwable
            else
                throw InsertEntityException(cause = throwable, message = "Something went wrong during save draft sync error", orReplace = orReplace)
        }
    }

    suspend fun findAllSyncErrorKeysByErrorState(syncErrorState: SyncErrorState, limit: Int): List<String> {
        return syncErrorDao.findAllIdsByErrorState(syncErrorState, limit)
    }

    suspend fun findAllByErrorStates(syncErrorStates: List<SyncErrorState>, offset: Int, limit: Int): List<SyncError> {
        return syncErrorDao.findAllByErrorStates(syncErrorStates, offset, limit).map { it.toDomain() }
    }

    suspend fun updateSyncErrorState(syncErrorState: SyncErrorState, syncErrors: List<SyncErrorMetadata>) {
        val keys = syncErrors.map { it.key }
        suspend fun updateQuery(ids: List<String>) = syncErrorDao.updateAllErrorState(syncErrorState, ids)
        val countUpdated = chunkedQueryByIds(keys, ::updateQuery).sum()
        logDebug("updateSyncErrorState $syncErrorState ${syncErrors.size} => $countUpdated")
    }

    suspend fun countByErrorStates(syncErrorStates: List<SyncErrorState>): Long {
        return syncErrorDao.countByErrorStates(syncErrorStates)
    }

    override fun observeChanges() = syncErrorDao.observeChanges()
}