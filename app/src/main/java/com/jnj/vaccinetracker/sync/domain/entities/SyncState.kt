package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.sync.data.models.SyncDate

sealed class SyncState {
    object Idle : SyncState()
    object OnlineInSync : SyncState()
    object OnlineSyncing : SyncState()
    data class SyncComplete(val lastSyncDate: SyncDate) : SyncState()
    data class Offline(val lastSyncDate: SyncDate) : SyncState()
    data class OfflineOutOfSync(val lastSyncDate: SyncDate?) : SyncState()
    data class SyncError(val isInProgress: Boolean, val numberOfErrors: Long) : SyncState()
}

fun SyncState.inProgress() = this == SyncState.OnlineSyncing || (this is SyncState.SyncError && isInProgress)