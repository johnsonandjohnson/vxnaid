package com.jnj.vaccinetracker.sync.p2p.common.models

@Suppress("DataClassPrivateConstructor")
data class Port private constructor(val port: Int) {

    companion object {
        fun fromPort(port: Int): Port? {
            return if (port > 0) Port(port) else null
        }
    }

}