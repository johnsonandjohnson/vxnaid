package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

class InternetConnectivity @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsRepository: SyncSettingsRepository,
) {

    companion object {
        private val counter = Counters.InternetConnectivity
        private const val DEFAULT_INTERNET_STATE = true

        @Suppress("BlockingMethodInNonBlockingContext")
        fun hasIpAddress(url: String): Boolean {
            val urlFormatted = formatBackendUrl(url)
            return try {
                val address = InetAddress.getByName(urlFormatted)
                !address.equals("")
            } catch (e: UnknownHostException) {
                // no-op
                false
            } catch (ex: Exception) {
                logError("unknown error occurred while fetching ip address for $url", ex)
                false
            }
        }

        private fun formatBackendUrl(backendUrl: String): String {
            return backendUrl
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
                .removeSuffix("/")
        }

    }

    private val internetConnectivityFlow = MutableStateFlow(true)

    fun observeInternetConnectivity(intervalMs: Long = counter.POLL_INTERNET_DELAY): Flow<Boolean> {
        return listOf(flow {
            while (true) {
                internetConnectivityFlow.value = fetchIsInternetConnected()
                delay(intervalMs)
            }
        }, internetConnectivityFlow).flattenMerge()
    }


    fun isInternetConnected(): Boolean = internetConnectivityFlow.value

    suspend fun isInternetConnectedAccurate(defaultConnectedState: Boolean = DEFAULT_INTERNET_STATE): Boolean {
        internetConnectivityFlow.value = fetchIsInternetConnected(defaultConnectedState)
        val state = internetConnectivityFlow.value
        logInfo("isInternetConnectedAccurate: $state (defaultConnectedState:$defaultConnectedState, url=${syncSettingsRepository.getBackendUrlOrNull()})")
        return state
    }

    private suspend fun fetchIsInternetConnected(defaultConnectedState: Boolean = DEFAULT_INTERNET_STATE): Boolean = withContext(dispatchers.io) {
        syncSettingsRepository.getBackendUrlOrNull()?.let { backendUrl ->
            hasIpAddress(backendUrl)
        } ?: defaultConnectedState
    }
}
