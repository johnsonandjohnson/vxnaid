package com.jnj.vaccinetracker.common.data.encryption

import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.jnj.vaccinetracker.common.exceptions.EncryptionException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class AESEncryption @Inject constructor(
    private val encryptionKeyRepository: EncryptionKeyRepository,
    private val secureBytesGenerator: SecureBytesGenerator,
    private val dispatchers: AppCoroutineDispatchers,
    private val base64: Base64,
) {
    companion object {
        private const val CYPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val AES_ALGORITHM = "AES"

        /**
         * must always be 16 bytes because it's the fixed block size (128-bit) of AES
         */
        private const val IV_LEN = 16

        private const val MAJOR_VERSION: Byte = 1.toByte()
        private val VERSION_1_0: Version = Version(byteArrayOf(MAJOR_VERSION, 0.toByte()))

        /**
         * ASCII '$'
         */
        private const val SEPARATOR: Byte = 0x24
    }

    private val parser = Parser()

    /**
     * caching the secret key spec give huge performance boost
     */
    private suspend fun getOrGenerateSecretKeySpec(): SecretKeySpec {
        val secretKeyData = encryptionKeyRepository.getOrGenerateAesEncryptionSecretKey()
        return SecretKeySpec(secretKeyData, AES_ALGORITHM)
    }

    private fun createFreshIv(): ByteArray = secureBytesGenerator.nextBytes(IV_LEN)

    private suspend fun createCipher(opmode: Int, iv: ByteArray): Cipher {
        val ivParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(CYPHER_TRANSFORMATION)
        val secretKeySpec = getOrGenerateSecretKeySpec()
        cipher.init(opmode, secretKeySpec, ivParameterSpec)
        return cipher
    }

    suspend fun encrypt(strToEncrypt: String): String = withContext(dispatchers.computation) {
        encrypt(strToEncrypt.encodeToByteArray()).let { bytes -> base64.encode(bytes) }
    }

    suspend fun decrypt(strToDecrypt: String): String = withContext(dispatchers.computation) {
        val bytes: ByteArray = strToDecrypt.let { base64.decode(it) }
        decrypt(bytes).decodeToString()
    }

    suspend fun encrypt(bytesToEncrypt: ByteArray): ByteArray = withContext(dispatchers.computation) {
        try {
            require(bytesToEncrypt.isNotEmpty()) { "bytesToEncrypt must not be empty" }
            val cipher = createCipher(Cipher.ENCRYPT_MODE, createFreshIv())
            val iv: ByteArray = cipher.iv
            val encryptedContent = cipher.doFinal(bytesToEncrypt)
            require(encryptedContent.isNotEmpty()) { "encryptedContent must not be empty" }
            parser.encode(EncryptedBlob(iv, encryptedContent))
        } catch (exception: Exception) {
            throw EncryptionException("error during encrypt", exception)
        }
    }

    suspend fun decrypt(bytesToDecrypt: ByteArray): ByteArray = withContext(dispatchers.computation) {
        try {
            val encryptedBlob = parser.decode(bytesToDecrypt)
            require(encryptedBlob.encryptedContent.isNotEmpty()) { "error, encrypted content is empty" }
            createCipher(Cipher.DECRYPT_MODE, iv = encryptedBlob.iv).doFinal(encryptedBlob.encryptedContent)
        } catch (exception: Exception) {
            throw EncryptionException("error during decrypt", exception)
        }
    }

    data class Version(val versionBytes: ByteArray) {
        override fun toString(): String {
            return versionBytes.decodeToString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Version

            if (!versionBytes.contentEquals(other.versionBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return versionBytes.contentHashCode()
        }
    }


    /**
     * Based on bcrypt.
     *
     * it will try to read/write following parts:
     * - version
     * - IV
     * - encrypted content
     *
     * [SEPARATOR] will be used between parts and before version. like so:
     *
     * $version$IV$encryptedContent
     *
     * the version consists of 2 bytes. with the first byte being major version and second byte minor version.
     *
     * The versioning system offers optional backwards compatibility but the main purpose is validation.
     * We want to throw a parsing exception instead of an encryption exception if the IV is missing.
     * So since IV is random, we have to use some constant identifier as a prefix to know how to parse it.
     */
    class Version10Parser : Parser {
        companion object {
            private val VERSION = VERSION_1_0
            private const val VERSION_LENGTH = 2

            private const val MIN_CONTENT_LENGTH = 1

            /**
             * 1 represents [SEPARATOR]
             */
            private const val MIN_LENGTH = 1 + VERSION_LENGTH + 1 + IV_LEN + 1 + MIN_CONTENT_LENGTH
        }

        override fun decode(encodedBytes: ByteArray): EncryptedBlob {
            require(encodedBytes.isNotEmpty()) { "encodedBytes must not be empty" }
            require(encodedBytes.size >= MIN_LENGTH) { "encodedBytes length must be >= $MIN_LENGTH but was ${encodedBytes.size}" }
            var drop = 0
            fun nextBytes(length: Int): ByteArray {
                return encodedBytes.drop(drop).take(length).also {
                    if (length != Int.MAX_VALUE)
                        drop += length
                }.toByteArray()
            }

            fun validateSeparator(name: String) {
                val separatorByte = nextBytes(1).first()
                require(separatorByte == SEPARATOR) { "$name must be SEPARATOR: $SEPARATOR but got $separatorByte" }
            }

            validateSeparator("first byte")
            val version = Version(nextBytes(VERSION_LENGTH))
            require(version == VERSION) { "error, version mismatch: must be $VERSION but got $version" }
            validateSeparator("byte after version")
            val iv = nextBytes(IV_LEN)
            require(iv.size == IV_LEN) { "error, iv must have length $IV_LEN but got ${iv.size}" }
            validateSeparator("byte after iv")
            val content = nextBytes(Int.MAX_VALUE)
            return EncryptedBlob(iv, content)
        }

        override fun encode(encryptedBlob: EncryptedBlob): ByteArray {
            return byteArrayOf(SEPARATOR) + listOf(VERSION.versionBytes, encryptedBlob.iv, encryptedBlob.encryptedContent).reduce { acc, bytes -> acc + SEPARATOR + bytes }
        }

    }

    interface Parser {
        fun decode(encodedBytes: ByteArray): EncryptedBlob
        fun encode(encryptedBlob: EncryptedBlob): ByteArray

        companion object {
            operator fun invoke() = Version10Parser()
        }
    }

    /**
     * we prefix each encrypted bytes with a random [iv] to make each file always unique
     */
    class EncryptedBlob(val iv: ByteArray, val encryptedContent: ByteArray) {
        init {
            require(iv.size == IV_LEN) { "iv must have length $IV_LEN" }
        }
    }
}
