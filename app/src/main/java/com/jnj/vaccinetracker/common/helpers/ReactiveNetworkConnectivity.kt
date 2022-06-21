package com.jnj.vaccinetracker.common.helpers

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ReactiveNetworkConnectivity @Inject constructor(
    private val context: Context,
    private val networkStateMonitor: NetworkStateMonitor,
    private val reactiveAirplaneMode: ReactiveAirplaneMode
) {

    private fun Connectivity.reduce(airplaneMode: Boolean) = if (airplaneMode) Connectivity.disconnected() else this

    val connectivity: Connectivity get() = networkStateMonitor.connectivity.reduce(airplaneMode = reactiveAirplaneMode.isAirplaneModeOn(context))

    fun observeNetworkConnectivity(): Flow<Connectivity> = networkStateMonitor.observeNetworkConnectivity()
        .onEach {
            logInfo("observeNetworkConnectivity: {}", it)
        }
        .combine(reactiveAirplaneMode.getAndObserve(context)) { connectivity, isAirplaneMode ->
            connectivity.reduce(airplaneMode = isAirplaneMode)
        }.distinctUntilChanged()
}