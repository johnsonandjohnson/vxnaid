package com.jnj.vaccinetracker.common.domain.usecases.masterdata.base

import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.exceptions.GetMasterDataRemoteException
import com.jnj.vaccinetracker.common.exceptions.MapMasterDataDomainException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class GetMasterDataUseCaseBase<DTO, DOM>() {
    protected abstract suspend fun getMasterDataPersistedCache(): DTO?
    protected abstract suspend fun getMasterDataRemote(): DTO
    protected abstract val dispatchers: AppCoroutineDispatchers
    protected abstract val syncLogger: SyncLogger
    protected abstract val masterDataFile: MasterDataFile
    protected abstract val masterDataRepository: MasterDataRepository

    protected abstract fun getMemoryCache(): DOM?
    protected abstract fun setMemoryCache(memoryCache: DOM?)

    private val mutex = Mutex()

    private val job = SupervisorJob()

    protected val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    var lastDateModifiedFromPersistedFile: SyncDate? = null

    protected abstract suspend fun DTO.toDomain(): DOM

    private val MasterDataFile.dateModified get() = masterDataRepository.getDateModified(this)


    private suspend fun onPersistedMasterDataChanged() {
        logInfo("onPersistedMasterDataChanged latest $masterDataFile")
        if (getMemoryCache() != null && masterDataFile.dateModified == lastDateModifiedFromPersistedFile) {
            logInfo("onPersistedMasterDataChanged already loaded this persisted $masterDataFile $lastDateModifiedFromPersistedFile")
            return
        }

        val persistedData = getMasterDataPersistedCacheOrNull()
        if (persistedData != null)
            lastDateModifiedFromPersistedFile = masterDataFile.dateModified
    }

    private fun observePersistedMasterData() = scope.launch(dispatchers.io) {
        syncLogger.observeMasterDataPersisted(masterDataFile)
            .collectLatest { onPersistedMasterDataChanged() }
    }

    protected fun initState() {
        observePersistedMasterData()
    }

    /**
     * this method should never throw an [Exception]
     */
    private suspend fun getMasterDataPersistedCacheOrNull(): DOM? {
        logInfo("getMasterDataPersistedCacheOrNull $masterDataFile")
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.READ_PERSISTED_MASTER_DATA)
        val masterDataPersisted = try {
            getMasterDataPersistedCache()?.also { _ ->
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            syncLogger.logSyncError(syncErrorMetadata, ex)
            val message = "failed to get master data persisted cache for $masterDataFile"
            logError(message, ex)
            // don't rethrow exception!!
            null
        }
        return kotlin.runCatching {
            masterDataPersisted?.toDomainOrThrow()
        }.getOrNull()?.also { dom ->
            setMemoryCache(dom)
        }
    }

    private suspend fun DTO.toDomainOrThrow(): DOM {
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.MAP_MASTER_DATA)
        return try {
            toDomain().also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            syncLogger.logSyncError(syncErrorMetadata, ex)
            val message = "failed to map master data to domain: $masterDataFile"
            logError(message, ex)
            throw MapMasterDataDomainException(message, ex)
        }
    }

    private suspend fun getMasterDataRemoteOrThrow(): DOM {
        logInfo("getMasterDataRemoteOrThrow")
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.GET_MASTER_DATA_CALL)
        val masterDataRemote = try {
            getMasterDataRemote().also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            syncLogger.logSyncError(syncErrorMetadata, ex)
            val message = "failed to get master data remote: $masterDataFile"
            logError(message, ex)
            throw GetMasterDataRemoteException(message, ex)
        }
        return masterDataRemote.toDomainOrThrow().also { dom ->
            setMemoryCache(dom)
        }
    }

    suspend fun getMasterData(): DOM {
        logInfo("getMasterData $masterDataFile")
        return withContext(dispatchers.io) {
            mutex.withLock {
                val result = getMemoryCache() ?: getMasterDataPersistedCacheOrNull() ?: getMasterDataRemoteOrThrow()
                result
            }
        }
    }
}