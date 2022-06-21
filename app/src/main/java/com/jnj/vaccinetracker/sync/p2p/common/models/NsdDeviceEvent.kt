package com.jnj.vaccinetracker.sync.p2p.common.models

sealed class NsdDeviceEvent {
    data class Resolved(val nsdDevice: CompatibleNsdDevice) : NsdDeviceEvent()
    data class Lost(val nsdDevice: CompatibleNsdDevice) : NsdDeviceEvent()
}