package com.jnj.vaccinetracker.common.data.encryption

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureBytesGenerator @Inject constructor() {

    private val secureRandom by lazy {
        SecureRandom()
    }

    fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }
}