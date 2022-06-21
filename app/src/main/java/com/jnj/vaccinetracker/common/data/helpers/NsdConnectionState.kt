package com.jnj.vaccinetracker.common.data.helpers

import com.jnj.vaccinetracker.common.helpers.await
import com.jnj.vaccinetracker.common.helpers.logDebug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsdConnectionState @Inject constructor() {

    private val isConnected = MutableStateFlow(false)

    fun observeIsNsdConnected(): Flow<Boolean> = isConnected

    val isNsdConnected get() = isConnected.value

    suspend fun awaitNsdDisconnected(debugLabel: String) {
        if (debugLabel.isNotEmpty())
            logDebug("$debugLabel - awaitNsdDisconnected")
        observeIsNsdConnected().filter { connected -> !connected }.await()
    }

    fun onNsdConnected() {
        isConnected.value = true
    }

    fun onNsdDisconnected() {
        isConnected.value = false
    }

}