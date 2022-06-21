package com.jnj.vaccinetracker.common.data.encryption

import android.os.Build
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

class AESEncryptionKeyFactory @Inject constructor(
    private val secretKeyAlgorithmProvider: SecretKeyAlgorithmProvider,
    private val secureBytesGenerator: SecureBytesGenerator,
    private val securePasswordGenerator: SecurePasswordGenerator,
) {

    companion object {
        private const val SALT_LEN = 16
        private const val ITERATION_COUNT = 10_000
        private const val KEY_LEN = 256
        private const val PASSWORD_LEN = 32
    }

    private fun generatePassword() = securePasswordGenerator.generateSecurePassword(PASSWORD_LEN, upperCase = true, lowerCase = true, numbers = true, specialCharacters = true)
    private fun generateSalt() = secureBytesGenerator.nextBytes(SALT_LEN)

    fun generateSecretKey(
        password: String = generatePassword(),
        salt: ByteArray = generateSalt(),
        osApiLevel: Int = Build.VERSION.SDK_INT,
    ): ByteArray {
        require(password.isNotEmpty()) { "password must not be empty" }
        require(salt.isNotEmpty()) { "salt must not be empty" }
        val factory = SecretKeyFactory.getInstance(secretKeyAlgorithmProvider.getSecretKeyAlgorithm(osApiLevel))
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LEN)
        val secretKey = factory.generateSecret(spec)
        return requireNotNull(secretKey.encoded) { "secretKey.encoded must not be null" }
    }
}