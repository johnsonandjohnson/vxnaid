package com.jnj.vaccinetracker.sync.p2p.common.models

import android.net.nsd.NsdServiceInfo
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.MessageHeaders
import java.net.InetAddress

data class CompatibleNsdDevice(
    val host: String?,
    val port: Port?,
    val deviceName: String
) {
    companion object {

        fun NsdServiceInfo.toNsdDevice(): CompatibleNsdDevice? {
            val compatibleService =
                CompatibleService.tryParse(serviceName = serviceName) ?: return null
            val inetAddress: InetAddress? = host
            return CompatibleNsdDevice(
                host = Host.fromInetAddress(inetAddress)?.hostAddress,
                port = Port.fromPort(port),
                deviceName = compatibleService.deviceName
            )
        }

        fun MessageHeaders.toNsdDevice(): CompatibleNsdDevice {
            return CompatibleNsdDevice(
                host = deviceIp,
                port = Port.fromPort(devicePort),
                deviceName = deviceName
            )
        }
    }

    val compatibleService get() = CompatibleService.fromDeviceName(deviceName)
}