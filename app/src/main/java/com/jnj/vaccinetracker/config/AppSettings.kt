package com.jnj.vaccinetracker.config

import com.jnj.vaccinetracker.common.helpers.*

enum class Flavor {
    /**
     * to be used by developers only in debug mode.
     */
    DEV,

    /**
     * Same as [DEV] but with mock build type. Has additional testing/performance features
     */
    MOCK,

    /**
     * Test flavor for during UAT and SIT
     */
    UAT,

    /**
     * Production flavor to be used by clients
     */
    PROD,
}


data class AppSettings(
    val flavor: Flavor,
    val logConfig: LogConfig,
    val automaticLicenseActivation: Boolean,
    val showDeleteSyncErrorsButton: Boolean,
    val showSyncNowButton: Boolean,
    val showShareSyncErrorsButton: Boolean,
    val showManualLicenseActivationButton: Boolean,
) {

    val manualLicenseActivationRequired = !automaticLicenseActivation

    val showShareLogsButton = logConfig.fileLoggingEnabled
    val showClearLogsButton: Boolean = showShareLogsButton && isUatOrLess

    companion object {
        val DEV = AppSettings(
            Flavor.DEV,
            automaticLicenseActivation = false,
            showDeleteSyncErrorsButton = true,
            showSyncNowButton = true,
            showShareSyncErrorsButton = true,
            showManualLicenseActivationButton = true,
            logConfig = LogConfig.DEFAULT
        )
        val UAT = AppSettings(
            Flavor.UAT,
            automaticLicenseActivation = false,
            showDeleteSyncErrorsButton = true,
            showSyncNowButton = true,
            showShareSyncErrorsButton = true,
            showManualLicenseActivationButton = true,
            logConfig = LogConfig.DEFAULT
        )
        val PROD = AppSettings(
            Flavor.PROD,
            automaticLicenseActivation = true,
            showDeleteSyncErrorsButton = false,
            showSyncNowButton = true,
            showShareSyncErrorsButton = true,
            showManualLicenseActivationButton = true,
            logConfig = LogConfig(listOf(LogTarget.File(LogPriority.ERROR)))
        )

        /**
         * set [AppSettings.logConfig] to [LogConfig.NONE] if you want a small performance boost
         */
        val MOCK = DEV.copy(flavor = Flavor.MOCK, logConfig = LogConfig.DEFAULT)
    }

    val isUatOrLess: Boolean get() = flavor.ordinal <= Flavor.UAT.ordinal

    @Suppress("unused")
    val isMock: Boolean
        get() = flavor == Flavor.MOCK
}

val appSettings: AppSettings = when {
    isMockBackendBuildType -> AppSettings.MOCK
    isDebugMode -> AppSettings.DEV
    else -> {
        /**
         * set to [AppSettings.PROD] for production release, otherwise [AppSettings.UAT]
         */
        AppSettings.PROD
    }
}