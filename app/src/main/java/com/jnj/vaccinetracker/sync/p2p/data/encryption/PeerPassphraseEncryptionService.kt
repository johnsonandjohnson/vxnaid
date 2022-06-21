package com.jnj.vaccinetracker.sync.p2p.data.encryption

import com.jnj.vaccinetracker.common.data.encryption.AESEncryptionKeyFactory
import com.jnj.vaccinetracker.common.data.encryption.SecureBytesGenerator
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.EncryptedSecret
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class PeerPassphraseEncryptionService @Inject constructor(
    private val aesEncryptionKeyFactory: AESEncryptionKeyFactory,
    private val dispatchers: AppCoroutineDispatchers,
    private val secureBytesGenerator: SecureBytesGenerator,
    private val base64: Base64,
) {
    companion object {
        private const val AES_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val SALT_LEN = 16
        private const val IV_LEN = 16
    }

    private fun createSecretKey(
        credentials: SyncUserCredentials,
        salt: ByteArray,
        osApiLevel: Int,
    ): SecretKeySpec {
        val secretKeyData =
            aesEncryptionKeyFactory.generateSecretKey(
                password = credentials.toString(),
                salt = salt, osApiLevel = osApiLevel
            )
        return SecretKeySpec(secretKeyData, AES_ALGORITHM)
    }

    private fun createCipher(secretKey: SecretKeySpec, iv: ByteArray, opmode: Int): Cipher {
        val ivParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(opmode, secretKey, ivParameterSpec)
        return cipher
    }

    private suspend fun ByteArray.base64() = base64.encode(this)
    private suspend fun String.toByteArray() = base64.decode(this)

    /**
     * @return encrypted passphrase
     */
    suspend fun encrypt(
        credentials: SyncUserCredentials,
        osApiLevel: Int,
        passphrase: String,
    ): EncryptedSecret =
        withContext(dispatchers.io) {
            val salt = secureBytesGenerator.nextBytes(SALT_LEN)
            val iv = secureBytesGenerator.nextBytes(IV_LEN)
            val cipher = createCipher(
                createSecretKey(credentials, salt, osApiLevel),
                iv,
                Cipher.ENCRYPT_MODE
            )
            val output = cipher.doFinal(passphrase.encodeToByteArray())
            EncryptedSecret(secret = output.base64(), salt = salt.base64(), iv = iv.base64())
        }

    /**
     * @return decrypted passphrase
     */
    suspend fun decrypt(
        credentials: SyncUserCredentials,
        osApiLevel: Int,
        encryptedPassphrase: EncryptedSecret,
    ): String =
        withContext(dispatchers.io) {
            val iv = encryptedPassphrase.iv.toByteArray()
            val salt = encryptedPassphrase.salt.toByteArray()
            val cipher = createCipher(
                createSecretKey(credentials, salt, osApiLevel),
                iv,
                Cipher.DECRYPT_MODE
            )
            val output = cipher.doFinal(encryptedPassphrase.secret.toByteArray())
            output.decodeToString()
        }
}