package com.jnj.vaccinetracker.common.data.managers

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class IrisScannerConnectionManager @Inject constructor() {

    private var hasRecentlyConnectedDevice = false

    fun onDeviceConnected() {
        hasRecentlyConnectedDevice = true
    }

    fun hasRecentlyConnectedDevice(): Boolean {
        return hasRecentlyConnectedDevice
    }

    fun resetRecentlyConnectedDevice() {
        hasRecentlyConnectedDevice = false
    }

}