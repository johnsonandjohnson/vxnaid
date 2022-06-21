package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.common.helpers.logInfo
import com.neurotec.biometrics.NMatchingSpeed
import com.neurotec.biometrics.NTemplateSize
import com.neurotec.biometrics.client.NBiometricClient
import dagger.Module
import dagger.Provides

/**
 * @author druelens
 * @version 1
 */
@Module
class BiometricsModule {

    private companion object {
        private const val IRIS_MAX_ROTATION = 15F
        private const val IRIS_QUALITY_THRESHOLD = 5
    }

    @Provides
            /**
             * don't make singleton!!
             */
    fun provideBiometricsClient(): NBiometricClient {
        return NBiometricClient().apply {
            // Set iris scan preferences
            irisesMatchingSpeed = NMatchingSpeed.LOW
            irisesMaximalRotation = IRIS_MAX_ROTATION
            irisesTemplateSize = NTemplateSize.LARGE
            irisesQualityThreshold = IRIS_QUALITY_THRESHOLD.toByte()
            isIrisesFastExtraction = false
            logInfo("Iris scan preferences set")

            // Initialize the client
            isUseDeviceManager = true
            initialize()
        }
    }
}