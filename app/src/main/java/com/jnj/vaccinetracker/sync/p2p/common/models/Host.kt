package com.jnj.vaccinetracker.sync.p2p.common.models

import java.net.InetAddress

@Suppress("DataClassPrivateConstructor")
data class Host private constructor(val hostAddress: String) {

    companion object {
        fun fromInetAddress(inetAddress: InetAddress?): Host? {
            val hostAddress: String? = inetAddress?.hostAddress
            return if (!hostAddress.isNullOrEmpty() && hostAddress != "::") {
                return Host(hostAddress)
            } else {
                null
            }
        }
    }
}