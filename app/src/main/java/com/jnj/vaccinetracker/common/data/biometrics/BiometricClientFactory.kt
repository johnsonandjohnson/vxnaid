package com.jnj.vaccinetracker.common.data.biometrics

import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.di.BiometricsModule
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import javax.inject.Inject

class BiometricClientFactory @Inject constructor(private val dispatchers: AppCoroutineDispatchers, private val participantDataFileIO: ParticipantDataFileIO) {

    fun create(): BiometricClient {
        val nBiometricClient = BiometricsModule().provideBiometricsClient()
        return BiometricClient(nBiometricClient, dispatchers, participantDataFileIO)
    }
}