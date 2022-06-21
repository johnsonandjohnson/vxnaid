package com.jnj.vaccinetracker.sync.p2p.common.models

data class NsdSession(
    val sessionToken: String,
    val device: CompatibleNsdDevice,
    val username: String
)