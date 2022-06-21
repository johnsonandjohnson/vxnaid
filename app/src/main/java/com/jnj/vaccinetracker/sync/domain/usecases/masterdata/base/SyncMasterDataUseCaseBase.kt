package com.jnj.vaccinetracker.sync.domain.usecases.masterdata.base

import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.exceptions.GetMasterDataRemoteException
import com.jnj.vaccinetracker.common.exceptions.StoreMasterDataException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger

interface SyncMasterDataUseCase {
    suspend fun sync(dateModified: SyncDate)
}

abstract class SyncMasterDataUseCaseBase<T> : SyncMasterDataUseCase {

    protected abstract suspend fun getMasterDataRemote(): T
    protected abstract suspend fun storeMasterData(masterData: T)

    protected abstract val syncLogger: SyncLogger
    protected abstract val masterDataRepository: MasterDataRepository
    protected abstract val masterDataFile: MasterDataFile

    private suspend fun getMasterDataRemoteOrThrow(): T {
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.GET_MASTER_DATA_CALL)
        return try {
            getMasterDataRemote().also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (throwable: Throwable) {
            syncLogger.logSyncError(syncErrorMetadata, throwable)
            throw GetMasterDataRemoteException("getMasterDataRemote error $masterDataFile", throwable)
        }
    }

    private suspend fun storeMasterDataOrThrow(masterData: T, dateModified: SyncDate) {
        val syncErrorMetadata = SyncErrorMetadata.MasterData(masterDataFile, SyncErrorMetadata.MasterData.Action.PERSIST_MASTER_DATA)
        try {
            storeMasterData(masterData)
            masterDataRepository.storeDateModifiedOrThrow(masterDataFile, dateModified).also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }
        } catch (throwable: Throwable) {
            syncLogger.logSyncError(syncErrorMetadata, throwable)
            throw StoreMasterDataException("storeMasterData error $masterDataFile", throwable)
        }
    }


    /**
     * first [getMasterDataRemote] then [storeMasterData] and attach [dateModified] as file metadata
     */
    override suspend fun sync(dateModified: SyncDate) {
        logInfo("sync $this")
        val masterData = getMasterDataRemoteOrThrow()
        storeMasterDataOrThrow(masterData, dateModified)
    }
}