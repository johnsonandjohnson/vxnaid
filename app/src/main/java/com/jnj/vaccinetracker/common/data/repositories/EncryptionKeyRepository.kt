package com.jnj.vaccinetracker.common.data.repositories

import com.jnj.vaccinetracker.common.data.encryption.AESEncryptionKeyFactory
import com.jnj.vaccinetracker.common.data.encryption.DatabasePassphraseGenerator
import com.jnj.vaccinetracker.common.data.encryption.SecretSharedPreferences
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionKeyRepository @Inject constructor(
    private val secretSharedPreferences: SecretSharedPreferences,
    private val aesEncryptionKeyFactory: AESEncryptionKeyFactory,
    private val databasePassphraseGenerator: DatabasePassphraseGenerator,
    private val base64: Base64,
) {
    private val mutex = Mutex()

    companion object {
        private const val PREF_DATABASE_PASSPHRASE = "database_passphrase"
        private const val PREF_AES_ENCRYPTION_SECRET_KEY = "aes_encryption_secret_key"
    }

    /**
     * @return passphrase stored in [SecretSharedPreferences] with given [key] if it exists otherwise [generate] a new one and store it
     */
    private suspend fun getOrGenerate(key: String, generate: suspend () -> String): String {
        mutex.withLock {
            return secretSharedPreferences.getString(key) ?: run {
                val generatedPassphrase = generate()
                secretSharedPreferences.putString(key, generatedPassphrase)
                val storedPassphrase = secretSharedPreferences.getString(key)
                if (storedPassphrase == generatedPassphrase) {
                    storedPassphrase
                } else {
                    secretSharedPreferences.remove(key)
                    throw Exception("storedPassphrase!=generatedPassphrase for [$key]")
                }
            }
        }
    }

    suspend fun setDatabasePassphrase(passphrase: String) {
        logInfo("setDatabasePassphrase")
        mutex.withLock {
            secretSharedPreferences.putString(PREF_DATABASE_PASSPHRASE, passphrase)
        }
    }

    suspend fun getOrGenerateDatabasePassphrase(): String {
        return getOrGenerate(PREF_DATABASE_PASSPHRASE) {
            databasePassphraseGenerator.generatePassphrase()
        }
    }

    suspend fun setAesEncryptionSecretKey(secretKey: ByteArray) {
        logInfo("setAesEncryptionSecretKey")
        mutex.withLock {
            secretSharedPreferences.putString(PREF_AES_ENCRYPTION_SECRET_KEY, secretKey.let { base64.encode(it) })
        }
    }

    suspend fun getOrGenerateAesEncryptionSecretKey(): ByteArray {
        return getOrGenerate(PREF_AES_ENCRYPTION_SECRET_KEY) {
            aesEncryptionKeyFactory.generateSecretKey().let { base64.encode(it) }
        }.let { base64.decode(it) }
    }
}