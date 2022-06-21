package com.jnj.vaccinetracker.sync.domain.helpers

import com.jnj.vaccinetracker.sync.domain.entities.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateObserver @Inject constructor() {
    private val syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    fun observeSyncState(): Flow<SyncState> = syncState

    fun emit(syncState: SyncState) {
        this.syncState.value = syncState
    }
}