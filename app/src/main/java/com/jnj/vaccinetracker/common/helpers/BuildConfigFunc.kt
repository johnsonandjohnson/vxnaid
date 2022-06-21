package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.BuildConfig

val isDebugMode = BuildConfig.DEBUG
const val appVersion: String = BuildConfig.VERSION_NAME
const val isMockBackendBuildType = BuildConfig.BUILD_TYPE == "mockBackend"
val isManualFlavor = BuildConfig.FLAVOR.contains("manual")