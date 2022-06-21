package com.jnj.vaccinetracker.common.data.encryption

import android.os.Build
import javax.inject.Inject

class SecretKeyAlgorithmProvider @Inject constructor() {

    companion object {
        const val SECRET_KEY_ALGORITHM_LEGACY = "PBKDF2WithHmacSHA1"
        const val SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    fun getSecretKeyAlgorithm(osApiLevel: Int) = if (osApiLevel.coerceAtMost(Build.VERSION.SDK_INT) >= Build.VERSION_CODES.O) SECRET_KEY_ALGORITHM else SECRET_KEY_ALGORITHM_LEGACY
}