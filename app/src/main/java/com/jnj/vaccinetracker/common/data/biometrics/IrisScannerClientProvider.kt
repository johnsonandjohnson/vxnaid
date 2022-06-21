package com.jnj.vaccinetracker.common.data.biometrics

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.neurotec.biometrics.client.NBiometricClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class IrisScannerClientProvider @Inject constructor(private val biometricsClientFactory: Provider<NBiometricClient>, private val dispatchers: AppCoroutineDispatchers) {
    private var _biometricClient: NBiometricClient? = null
    private val mutex = Mutex()

    suspend fun provideClient(): NBiometricClient = withContext(dispatchers.io) {
        mutex.withLock {
            _biometricClient ?: biometricsClientFactory.get().also { _biometricClient = it }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            _biometricClient?.irisScanner = null
            _biometricClient?.dispose()
            _biometricClient = null
        }
    }
}