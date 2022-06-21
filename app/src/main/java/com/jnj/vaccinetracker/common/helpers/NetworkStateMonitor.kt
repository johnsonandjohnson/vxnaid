package com.jnj.vaccinetracker.common.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class NetworkStateMonitor @Inject constructor(private val context: Context, private val dispatchers: AppCoroutineDispatchers) {
    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.main + job)
    private val connectivitySubject = MutableStateFlow(Connectivity.disconnected())
    private var networkCallback: NetworkCallback? = null
    private var idleReceiver: BroadcastReceiver? = null

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        initState()
    }

    private fun initState() {
        val networkCallback = createNetworkCallback().also { this.networkCallback = it }
        idleReceiver = registerIdleReceiver()
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun observeNetworkConnectivity(): Flow<Connectivity> {
        return connectivitySubject
    }

    val connectivity get() = connectivitySubject.value

    private fun registerIdleReceiver(): BroadcastReceiver? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val idleReceiver = createIdleBroadcastReceiver()
            val filter =
                IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            context.registerReceiver(idleReceiver, filter)
            idleReceiver
        } else {
            null
        }
    }

    private fun createIdleBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val idle = isIdleMode(context)
                connectivitySubject.value = connectivitySubject.value.copy(isIdleMode = idle)
            }
        }
    }

    private fun isIdleMode(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = context.packageName
            val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringOptimizations =
                manager.isIgnoringBatteryOptimizations(packageName)

            return manager.isDeviceIdleMode && !isIgnoringOptimizations
        } else {
            false
        }
    }

    private fun tryToUnregisterCallback() {
        try {
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (exception: Exception) {
            logError("could not unregister network callback", exception)
        }
    }

    private fun tryToUnregisterReceiver() {
        try {
            idleReceiver?.let { context.unregisterReceiver(it) }
        } catch (exception: Exception) {
            logError("could not unregister receiver", exception)
        }
    }

    fun dispose() {
        tryToUnregisterCallback()
        tryToUnregisterReceiver()
    }

    @Suppress("DEPRECATION")
    private fun getActiveNetworkType(): Connectivity.Type {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return Connectivity.Type.UNKNOWN
            val actNw =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return Connectivity.Type.UNKNOWN
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Connectivity.Type.WIFI
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Connectivity.Type.MOBILE
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Connectivity.Type.ETHERNET
                else -> Connectivity.Type.UNKNOWN
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> Connectivity.Type.WIFI
                        ConnectivityManager.TYPE_MOBILE -> Connectivity.Type.MOBILE
                        ConnectivityManager.TYPE_ETHERNET -> Connectivity.Type.ETHERNET
                        else -> Connectivity.Type.UNKNOWN
                    }
                }
            } ?: Connectivity.Type.UNKNOWN
        }
    }

    private fun createConnectivity(): Connectivity {
        return Connectivity(getActiveNetworkType(), isIdleMode(context))
    }

    private fun onNetworkChanged(network: Network?) {
        scope.launch(dispatchers.io) {
            connectivitySubject.value = if (network != null) createConnectivity() else Connectivity.disconnected()
        }
    }

    private fun createNetworkCallback(): NetworkCallback {
        return object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                logInfo("onAvailable")
                onNetworkChanged(network)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                logInfo("onCapabilitiesChanged")
                onNetworkChanged(network)
            }

            override fun onLost(network: Network) {
                logInfo("onLost")
                onNetworkChanged(network)
            }

            override fun onUnavailable() {
                logInfo("onUnavailable")
                onNetworkChanged(null)
            }
        }
    }
}

data class Connectivity(val type: Type, val isIdleMode: Boolean) {
    enum class Type {
        ETHERNET, MOBILE, WIFI, UNKNOWN;

        companion object {
            fun notUnknown() = values().filter { it != UNKNOWN }
        }
    }

    fun isConnectedNotUnknown() = !isIdleMode && type in Type.notUnknown()

    fun isConnectedWifi() = !isIdleMode && type == Type.WIFI

    companion object {
        fun disconnected() = Connectivity(Type.UNKNOWN, false)
    }
}