package com.jnj.vaccinetracker.common

import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class NetworkConnectivityFake @Inject constructor() : NetworkConnectivity {
    override fun observeNetworkConnectivity(): Flow<Boolean> {
        return flowOf(true)
    }

    override suspend fun isConnectedFast(): Boolean {
        return true
    }

    override suspend fun isConnectedAccurate(): Boolean {
        return true
    }

    override suspend fun awaitFastInternet(debugLabel: String) {
        /*
        Empty override because we don't want to call/use the awaitFastInternet function.
         */
    }

    override suspend fun awaitWifi(debugLabel: String) {
        /*
        Empty override because we don't want to call/use the awaitWifi function.
         */
    }

    override suspend fun isConnectedWifi(): Boolean {
        return true
    }

    override suspend fun observeWifiConnectivity(): Flow<Boolean> {
        return flowOf(true)
    }

    override suspend fun requireFastInternet() {
        /*
        Empty override because isConnectedFast() will always return true.
         */
    }
}