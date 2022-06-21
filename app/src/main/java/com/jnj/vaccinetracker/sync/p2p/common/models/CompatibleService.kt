package com.jnj.vaccinetracker.sync.p2p.common.models

class CompatibleService private constructor(val serviceName: String) {
    companion object {
        const val PREFIX = "<VMP>_"
        private fun isCompatible(serviceName: String) = serviceName.startsWith(PREFIX)
        fun tryParse(serviceName: String): CompatibleService? {
            return if (isCompatible(serviceName)) {
                CompatibleService(serviceName)
            } else {
                null
            }
        }

        fun fromDeviceName(deviceName: String) = CompatibleService("$PREFIX$deviceName")
    }

    val deviceName get() = serviceName.removePrefix(PREFIX)


}