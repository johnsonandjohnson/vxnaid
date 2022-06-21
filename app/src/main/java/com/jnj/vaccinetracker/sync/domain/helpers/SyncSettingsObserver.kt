package com.jnj.vaccinetracker.sync.domain.helpers

import com.jnj.vaccinetracker.common.data.helpers.NsdConnectionState
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.await
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class SyncSettingsObserver @Inject constructor(
    private val syncSettingsRepository: SyncSettingsRepository,
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    private val dispatchers: AppCoroutineDispatchers,
    private val nsdConnectionState: NsdConnectionState,
) {

    fun isSyncSettingsAvailable() = syncSettingsRepository.getSiteUuid() != null && syncUserCredentialsRepository.areSyncUserCredentialsStored()
    fun isSiteSelectionAvailable() = syncSettingsRepository.getSiteUuid() != null
    fun isSyncCredentialsAvailable() = syncUserCredentialsRepository.areSyncUserCredentialsStored()
    fun isNsdConnected() = nsdConnectionState.isNsdConnected
    suspend fun awaitSyncSettingsAvailable(debugLabel: String) {
        logInfo("$debugLabel - awaitSyncSettingsAvailable")
        observeSyncSettingsChanges().await()
    }

    suspend fun awaitSyncCredentialsAvailable(debugLabel: String) {
        logInfo("$debugLabel - awaitSyncCredentialsAvailable")
        observeSyncCredentials().await()
    }

    suspend fun awaitNsdDisconnected(debugLabel: String) = nsdConnectionState.awaitNsdDisconnected(debugLabel)

    fun observeSiteSelectionChanges(): Flow<String> = syncSettingsRepository.observeSiteUuid()
        .filterNotNull().flowOn(dispatchers.io)

    fun observeSyncCredentials(): Flow<SyncUserCredentials> = syncUserCredentialsRepository.observeSyncUserCredentials().filterNotNull()


    fun observeNsdDisconnected(): Flow<Unit> = nsdConnectionState.observeIsNsdConnected().filter { connected -> !connected }.map { }

    /**
     * if some syncs settings are missing this will suspend until those settings are present again
     */
    fun observeSyncSettingsChanges() = combine(
        observeSiteSelectionChanges(),
        observeSyncCredentials(),
        observeNsdDisconnected()
    ) { _, _, _ ->
    }.flowOn(dispatchers.io)
}