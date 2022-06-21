package com.jnj.vaccinetracker.sync.p2p.common.models

sealed class WifiDirectDevice {
    abstract val deviceName: String
    abstract val deviceServerSocketIP: String
    abstract val deviceServerSocketPort: Int
    abstract fun copyWithIpAddress(ip: String): WifiDirectDevice
}


data class ClientWifiDirectDevice(
    override val deviceName: String,
    override val deviceServerSocketIP: String,
    override val deviceServerSocketPort: Int = 0
) : WifiDirectDevice() {

    override fun copyWithIpAddress(ip: String): ClientWifiDirectDevice {
        return copy(deviceServerSocketIP = ip)
    }
}

data class ServerWifiDirectDevice(
    override val deviceName: String,
    override val deviceServerSocketIP: String,
    override val deviceServerSocketPort: Int = 0,
) : WifiDirectDevice() {
    override fun copyWithIpAddress(ip: String): ServerWifiDirectDevice {
        return copy(deviceServerSocketIP = ip)
    }
}


