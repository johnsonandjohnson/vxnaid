package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.domain.entities.SyncState
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.helpers.SyncStateBuilder
import com.jnj.vaccinetracker.sync.domain.helpers.SyncStateObserver
import com.jnj.vaccinetracker.sync.domain.services.SyncStateService.Companion.counter
import com.jnj.vaccinetracker.sync.domain.usecases.error.ObserveSyncErrorsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * build [SyncState] every [counter.DELAY] and emit it to [syncStateObserver]
 */
class SyncStateService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val syncStateBuilder: SyncStateBuilder,
    private val syncStateObserver: SyncStateObserver,
    private val networkConnectivity: NetworkConnectivity,
    private val observeSyncErrorsUseCase: ObserveSyncErrorsUseCase,
    private val syncLogger: SyncLogger,
    private val syncSettingsObserver: SyncSettingsObserver,
) {

    companion object {
        private val counter = Counters.SyncState
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var buildStateJob: Job? = null

    fun start() {
        if (buildStateJob?.isActive != true) {
            buildStateJob = scope.launch {
                emitStatePeriodically()
            }
        }
    }

    private suspend fun emitSyncState(): SyncState {
        syncSettingsObserver.awaitNsdDisconnected("")
        val syncState: SyncState = syncStateBuilder.buildSyncState()
        syncStateObserver.emit(syncState)
        return syncState
    }

    @OptIn(FlowPreview::class)
    private suspend fun emitStatePeriodically() {
        while (true) {
            val syncState = try {
                emitSyncState()
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("emitStatePeriodically error", ex)
            }

            if (syncState is SyncState.SyncComplete) {
                flowOf(Unit).onEach { delay(counter.SYNC_COMPLETE_DURATION) }.await()
            } else {
                val delayFlow = flowOf(Unit).onEach { delay(counter.DELAY) }
                val observeErrorsFlow = runCatchingDbQuery { observeSyncErrorsUseCase.observeChanges().drop(1) }
                val connectivityFlow = networkConnectivity.observeNetworkConnectivity().drop(1)
                val syncProgressFlow = syncLogger.observeSyncInProgress().drop(1)
                listOfNotNull(delayFlow, observeErrorsFlow, connectivityFlow, syncProgressFlow).awaitAny()
            }
        }
    }
}