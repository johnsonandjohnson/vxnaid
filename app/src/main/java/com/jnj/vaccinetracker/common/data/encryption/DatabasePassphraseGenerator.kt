package com.jnj.vaccinetracker.common.data.encryption

import javax.inject.Inject

class DatabasePassphraseGenerator @Inject constructor(private val passwordGenerator: SecurePasswordGenerator) {

    companion object {
        private const val PASSPHRASE_LEN = 32
    }

    fun generatePassphrase() = passwordGenerator.generateSecurePassword(PASSPHRASE_LEN, upperCase = true, lowerCase = true, numbers = true, specialCharacters = true)
}