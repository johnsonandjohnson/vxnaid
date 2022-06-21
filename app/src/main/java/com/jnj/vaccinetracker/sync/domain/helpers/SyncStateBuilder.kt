package com.jnj.vaccinetracker.sync.domain.helpers

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.common.helpers.weeks
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.SyncState
import com.jnj.vaccinetracker.sync.domain.usecases.error.GetSyncErrorCountUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateBuilder @Inject constructor(
    private val networkConnectivity: NetworkConnectivity,
    private val syncLogger: SyncLogger,
    private val syncErrorCountUseCase: GetSyncErrorCountUseCase,
) {

    companion object {
        private val SYNC_DATE_STALE_TIME = 1.weeks
    }

    private var oldSyncDate: SyncDate? = syncLogger.getSyncCompletedDate()

    suspend fun buildSyncState(): SyncState {
        val syncDate = syncLogger.getSyncCompletedDate()
        try {
            if (syncDate != null && oldSyncDate != syncDate) {
                return SyncState.SyncComplete(syncDate)
            }
            val inProgress = syncLogger.isInProgress()
            val dateNow = dateNow()
            val syncErrorCount = syncErrorCountUseCase.syncErrorCount()
            fun SyncDate.timeElapsed() = dateNow.time - time
            fun SyncDate.isStale() = timeElapsed() > SYNC_DATE_STALE_TIME

            return if (networkConnectivity.isConnectedFast()) {
                when {
                    inProgress -> SyncState.OnlineSyncing
                    syncErrorCount > 0 -> {
                        SyncState.SyncError(false, syncErrorCount)
                    }
                    syncDate != null -> SyncState.OnlineInSync
                    else -> SyncState.Idle
                }
            } else {
                when {
                    syncErrorCount > 0 -> {
                        SyncState.SyncError(false, syncErrorCount)
                    }
                    syncDate == null || syncDate.isStale() -> SyncState.OfflineOutOfSync(syncDate)
                    else -> SyncState.Offline(syncDate)
                }
            }
        } finally {
            oldSyncDate = syncDate
        }
    }
}