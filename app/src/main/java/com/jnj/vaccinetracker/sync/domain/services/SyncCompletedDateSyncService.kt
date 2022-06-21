package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.exceptions.ReportSyncCompletedDateException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.usecases.synccompleted.ReportSyncCompleteUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCompletedDateSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val networkConnectivity: NetworkConnectivity,
    private val reportSyncCompleteUseCase: ReportSyncCompleteUseCase,
    private val syncLogger: SyncLogger,
    private val syncSettingsObserver: SyncSettingsObserver,
) {

    companion object {
        private val counter = Counters.SyncCompletedDateReporting
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var taskJob: Job? = null

    fun start() {
        if (taskJob?.isActive != true) {
            taskJob = scope.launch {
                doTaskPeriodically()
            }
        }
    }

    private suspend fun report() {
        syncSettingsObserver.awaitSyncSettingsAvailable(debugLabel())
        try {
            val success = reportSyncCompleteUseCase.reportSyncComplete()
            logInfo("sync completed date reported? $success")
        } catch (ex: ReportSyncCompletedDateException) {
            //no-op we expect this ex is already handled or logged
        }
    }

    private suspend fun doTaskPeriodically() {
        while (true) {
            try {
                report()
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("doTaskPeriodically error", ex)
            }
            val delayFlow = flowOf(Unit).onEach { delaySafe(counter.DELAY) }.flowOn(dispatchers.io)
            val observeSyncCompletedDate = syncLogger.observeSyncCompletedDate().drop(1).filterNotNull()
            listOf(delayFlow, observeSyncCompletedDate).awaitAny()
        }
    }
}