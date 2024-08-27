package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import javax.inject.Inject


interface NetworkConnectivity {
    fun observeNetworkConnectivity(): Flow<Boolean>

    /**
     * @return **true** if internet available **false**
     */
    suspend fun isConnectedFast(): Boolean

    /**
     * @return **true** if internet available by checking IP address of backend and health call does not return 500, otherwise **false**
     */
    suspend fun isConnectedAccurate(): Boolean

    /**
     * [debugLabel] is important to indicate where we are suspending the coroutine for potentially long periods of time.
     */
    suspend fun awaitFastInternet(debugLabel: String)

    /**
     * await device is connected to WIFI
     */
    suspend fun awaitWifi(debugLabel: String)

    suspend fun isConnectedWifi(): Boolean

    suspend fun observeWifiConnectivity(): Flow<Boolean>

    suspend fun requireFastInternet()
}

/**
 * In match screen and visit screen we combine backend call with local data.
 * In case backend is really slow, this could ruin app performance.
 */
class NetworkConnectivityDefault @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val reactiveNetworkConnectivity: ReactiveNetworkConnectivity,
    private val internetConnectivity: InternetConnectivity,
    private val serverHealthMeter: ServerHealthMeter,
) : NetworkConnectivity {

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private val isNetworkConnectedState = MutableStateFlow<Boolean?>(null)
    private val networkConnectivity = MutableStateFlow(Connectivity.disconnected())

    init {
        initState()
    }

    private fun initState() {
        combine(
            reactiveNetworkConnectivity.observeNetworkConnectivity().onEach {
                networkConnectivity.value = it
            },
            internetConnectivity.observeInternetConnectivity(),
            serverHealthMeter.observeIsHealthy(),
        ) { connectivity, internet, serverHealthy ->
            val hasNetwork = connectivity.isConnectedNotUnknown()
            val result = hasNetwork && internet && serverHealthy
            logInfo("combine networkconnectivity network=$hasNetwork {}, internet=$internet, serverHealthy=$serverHealthy => $result", connectivity)
            result
        }.flowOn(dispatchers.io)
            .onEach { isConnected ->
                isNetworkConnectedState.value = isConnected
                logInfo("connectivityChanged: isConnected: $isConnected")
            }
            .launchIn(scope)
    }

    override fun observeNetworkConnectivity(): Flow<Boolean> {
        return isNetworkConnectedState.filterNotNull()
    }

    override suspend fun isConnectedAccurate(): Boolean {
        logInfo("isConnectedAccurate")
        val isConnected = internetConnectivity.isInternetConnectedAccurate() && serverHealthMeter.isHealthyAccurate()
        isNetworkConnectedState.value = isConnected
        logInfo("isConnectedAccurate: $isConnected")
        return isConnected
    }

    /**
     * @return **true** if internet available and internet speed is fast enough to be usable, otherwise **false**
     */
    override suspend fun isConnectedFast(): Boolean {
        return isNetworkConnectedState.filterNotNull().first()
    }

    /**
     * [debugLabel] is important to indicate where we are suspending the coroutine for potentially long periods of time.
     */
    override suspend fun awaitFastInternet(debugLabel: String) {
        logInfo("$debugLabel - awaitFastInternet")
        isNetworkConnectedState
            .filterNotNull()
            .filter { isConnected -> isConnected }
            .await()
    }


    override suspend fun awaitWifi(debugLabel: String) {
        observeWifiConnectivity().filter { connected -> connected }.await()
    }

    override suspend fun isConnectedWifi(): Boolean {
        return networkConnectivity.value.isConnectedWifi()
    }

    override suspend fun observeWifiConnectivity(): Flow<Boolean> {
        return networkConnectivity.map { it.isConnectedWifi() }
    }

    override suspend fun requireFastInternet() {
        if (!isConnectedFast())
            throw NoNetworkException()
    }
}

