package com.jnj.vaccinetracker.timetracker.domain.services

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.minutes
import com.jnj.vaccinetracker.common.helpers.seconds
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.SyncState
import com.jnj.vaccinetracker.sync.domain.entities.inProgress
import com.jnj.vaccinetracker.sync.domain.helpers.SyncStateObserver
import com.jnj.vaccinetracker.sync.p2p.data.helpers.ClientProgressProvider
import com.jnj.vaccinetracker.sync.p2p.domain.entities.ClientProgress
import com.jnj.vaccinetracker.timetracker.data.P2PTimeTracker
import com.jnj.vaccinetracker.timetracker.data.SyncTimeTracker
import com.jnj.vaccinetracker.timetracker.data.TimeLeapBuilder
import com.jnj.vaccinetracker.timetracker.data.TimeTrackerReportWriter
import com.jnj.vaccinetracker.timetracker.data.datasources.LastTimestampAddedToReportDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeTrackerService @Inject constructor(
    private val syncTimeTracker: SyncTimeTracker, syncStateObserver: SyncStateObserver,
    private val dispatchers: AppCoroutineDispatchers,
    private val timeLeapBuilder: TimeLeapBuilder,
    private val p2PTimeTracker: P2PTimeTracker,
    private val timeTrackerReportWriter: TimeTrackerReportWriter,
    private val lastTimestampAddedToReportDataSource: LastTimestampAddedToReportDataSource,
    private val clientProgressProvider: ClientProgressProvider,
) {

    companion object {
        private val STORE_TIME_LEAP_INTERVAL = 10.minutes
    }

    private val mutex = Mutex()

    private val job = SupervisorJob()

    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var _syncDate: SyncDate? = null

    init {
        syncStateObserver.observeSyncState()
            .debounce(1000)
            .onEach {
                if (it.inProgress()) {
                    syncTimeTracker.startRecording()
                } else
                    syncTimeTracker.pauseRecording()
                if (it is SyncState.SyncComplete && _syncDate == null) {
                    _syncDate = it.lastSyncDate
                    storeTimeLeap()
                }
            }.launchIn(scope)

        syncTimeTracker.observeRecordedTime()
            .debounce(5.seconds)
            .filter { _syncDate == null }
            .filter { isReadyToStore(it) }
            .onEach {
                storeTimeLeap()
            }
            .launchIn(scope)

        var lastProgress: ClientProgress = ClientProgress.Idle

        clientProgressProvider.clientProgress.onEach { clientProgress ->
            if (clientProgress.isNotInProgress()) {
                p2PTimeTracker.pauseRecording()
            } else if (lastProgress.isIdle()) {
                p2PTimeTracker.clear()
                p2PTimeTracker.startRecording()
            }
            lastProgress = clientProgress
        }.launchIn(scope)
    }

    private fun isReadyToStore(timestamp: Long): Boolean {
        val lastTimestamp = lastTimestampAddedToReportDataSource.getLastTimestamp()
        return lastTimestamp + STORE_TIME_LEAP_INTERVAL < timestamp
    }

    private suspend fun storeTimeLeap() = mutex.withLock {
        logInfo("storeTimeLeap")
        val timeLeap = timeLeapBuilder.buildTimeLeap()
        timeTrackerReportWriter.appendTimeLeap(timeLeap)
        lastTimestampAddedToReportDataSource.storeLastTimestamp(timeLeap.timeStamp)
    }

}