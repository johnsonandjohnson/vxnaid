package com.jnj.vaccinetracker.sync.data.helpers

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.domain.helpers.ForceSyncObserver
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield
import javax.inject.Inject

class ServerPollUtil @Inject constructor(
    private val syncSettingsObserver: SyncSettingsObserver,
    private val networkConnectivity: NetworkConnectivity,
    private val forceSyncObserver: ForceSyncObserver,
    private val dispatchers: AppCoroutineDispatchers,
) {

    /**
     * when [pollServerBlock] returns **false**, the loop will be stopped
     */
    suspend fun pollServerPeriodically(
        delayMs: Long,
        debugLabel: String,
        skipDelayWhenComeBackOnline: Boolean = true,
        skipDelayWhenSyncSettingsChanged: Boolean = true,
        skipDelayWhenSyncCredentialsChanged: Boolean = false,
        pollServerBlock: suspend () -> Boolean,
    ) {
        val debugLabelWithPrefix = "${debugLabel()}.$debugLabel"
        while (true) {
            suspend fun doPoll(): Boolean {
                logInfo("$debugLabelWithPrefix - pollServerPeriodically")
                if (skipDelayWhenComeBackOnline)
                    networkConnectivity.awaitFastInternet(debugLabelWithPrefix)
                if (skipDelayWhenSyncSettingsChanged)
                    syncSettingsObserver.awaitSyncSettingsAvailable(debugLabelWithPrefix)
                if (skipDelayWhenSyncCredentialsChanged)
                    syncSettingsObserver.awaitSyncCredentialsAvailable(debugLabelWithPrefix)
                val shouldContinue = try {
                    pollServerBlock()
                } catch (ex: Throwable) {
                    yield()
                    ex.rethrowIfFatal()
                    logError("unknown pollServer error ($debugLabelWithPrefix)", ex)
                    true
                }
                if (!shouldContinue)
                    return false
                val flowList = mutableListOf<Flow<*>>()
                // delay flow
                flowList += flowOf(Unit).onEach {
                    logInfo("$debugLabel delayStart")
                    delaySafe(delayMs)
                    logInfo("$debugLabel delayDone")
                }.flowOn(dispatchers.io)
                if (skipDelayWhenComeBackOnline && !networkConnectivity.isConnectedFast()) {
                    // we only want to wait for internet if are offline right now.
                    flowList += networkConnectivity.observeNetworkConnectivity()
                        .filter { it }
                }
                if (skipDelayWhenSyncSettingsChanged) {
                    flowList += syncSettingsObserver.observeSiteSelectionChanges().let { flow ->
                        // if sync settings are present, we can skip the first element to only listen for changes
                        if (syncSettingsObserver.isSiteSelectionAvailable()) flow.drop(1) else flow
                    }
                }
                if (skipDelayWhenSyncCredentialsChanged) {
                    flowList += syncSettingsObserver.observeSyncCredentials().let { flow ->
                        if (syncSettingsObserver.isSyncCredentialsAvailable()) flow.drop(1) else flow
                    }
                }
                if (skipDelayWhenSyncSettingsChanged && syncSettingsObserver.isNsdConnected()) {
                    flowList += syncSettingsObserver.observeNsdDisconnected()
                }

                flowList += forceSyncObserver.observeForceSync()
                flowList.awaitAny()
                return true
            }

            val shouldContinue = doPoll()
            if (!shouldContinue) {
                break
            }
        }
        logInfo("$debugLabelWithPrefix - pollServerPeriodically discontinued")
    }
}