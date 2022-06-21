package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.domain.usecases.GetServerHealthUseCase
import com.jnj.vaccinetracker.config.Counters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerHealthMeter @Inject constructor(
    private val getServerHealthUseCase: GetServerHealthUseCase,
    private val dispatchers: AppCoroutineDispatchers,
) {
    companion object {
        private val counter = Counters.ServerHealthMeter
    }

    private val job = SupervisorJob()

    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private val isHealthy = MutableStateFlow(true)
    private val measuringEnabled = MutableStateFlow(false)

    fun observeIsHealthy(): Flow<Boolean> = isHealthy

    var measureAgainJob: Job? = null

    suspend fun isHealthyAccurate(): Boolean {
        logInfo("isHealthyAccurate")
        return measureIsHealthy()
    }

    private fun measureAgainLater() {
        logInfo("measureAgainLater")
        measureAgainJob?.cancel()
        measureAgainJob = scope.launch {
            delaySafe(counter.HEALTH_CALL_DELAY)
            if (measuringEnabled.value)
                measureIsHealthy()
        }
    }

    private suspend fun measureIsHealthy(): Boolean {
        val result = getServerHealthUseCase.getHealth()
        result.exceptionOrNull()?.let {
            logError("getHealth error", it)
        }
        val httpCode = result.getOrDefault(0)
        val serverHealthy = httpCode < 500
        isHealthy.value = serverHealthy
        measuringEnabled.value = !serverHealthy
        if (measuringEnabled.value) {
            measureAgainLater()
        }
        return serverHealthy.also {
            logInfo("measureIsHealthy: $it")
        }
    }

    suspend fun startMeasuring() {
        if (!isHealthy.value && measuringEnabled.value) {
            logWarn("startMeasuring skipped, we are offline and already measuring")
        } else {
            measureIsHealthy()
        }
    }

}