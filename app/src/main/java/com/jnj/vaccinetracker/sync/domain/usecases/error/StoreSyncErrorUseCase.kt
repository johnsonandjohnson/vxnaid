package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.exceptions.StoreSyncErrorException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.domain.entities.DraftSyncError
import com.jnj.vaccinetracker.sync.domain.entities.toPersistence
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class StoreSyncErrorUseCase @Inject constructor(
    private val syncErrorRepository: SyncErrorRepository,
    private val networkConnectivity: NetworkConnectivity,
    private val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsObserver: SyncSettingsObserver,
) {

    private suspend fun DraftSyncError.toPersistenceAsync() = withContext(dispatchers.io) {
        toPersistence()
    }

    /**
     * for example ignore certain exceptions you get from rest calls
     */
    private fun DraftSyncError.shouldIgnore(): Boolean {
        return when (stackTrace) {
            is NoNetworkException -> true
            else -> false
        }
    }

    suspend fun store(draftSyncError: DraftSyncError) {
        if (!networkConnectivity.isConnectedAccurate()) {
            logError("no network available, ignoring sync error", draftSyncError.stackTrace)
            return
        }
        if (!syncSettingsObserver.isSyncSettingsAvailable()) {
            logError("sync settings not available, ignoring sync error", draftSyncError.stackTrace)
            return
        }
        if (draftSyncError.shouldIgnore()) {
            logError("should ignore is true, ignoring sync error", draftSyncError.stackTrace)
            return
        }
        if (syncSettingsObserver.isNsdConnected()) {
            logError("nsd is connected, ignoring sync error", draftSyncError.stackTrace)
            return
        }
        val metadata = draftSyncError.metadata
        try {
            logInfo("saveSyncError: ${metadata.key} (${draftSyncError.stackTrace::class.simpleName})", draftSyncError.stackTrace)
            val syncError = draftSyncError.toPersistenceAsync()
            syncErrorRepository.insert(syncError, orReplace = true)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            throw StoreSyncErrorException("error storing syncError: ${metadata.key}", ex)
        }
    }
}