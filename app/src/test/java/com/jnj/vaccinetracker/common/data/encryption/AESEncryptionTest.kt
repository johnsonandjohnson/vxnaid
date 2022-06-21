package com.jnj.vaccinetracker.common.data.encryption

import TestBase64
import com.jnj.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.jnj.vaccinetracker.common.exceptions.EncryptionException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlin.random.Random

class AESEncryptionTest : FunSpec({

    val secureBytesGen = SecureBytesGenerator()
    val testDispatcher = TestCoroutineDispatcher()
    val base64 = TestBase64()

    val aesEncryptionKeyFactory = AESEncryptionKeyFactory(SecretKeyAlgorithmProvider(), secureBytesGen, SecurePasswordGenerator())
    fun mockSecureBytesGen(secureBytes: ByteArray): SecureBytesGenerator {
        val gen: SecureBytesGenerator = mockk()
        every { gen.nextBytes(any()) } returns secureBytes
        return gen
    }

    fun aes(
        password: String = "testPassword",
        salt: ByteArray = secureBytesGen.nextBytes(16),
        throwError: Boolean = false,
        rng: SecureBytesGenerator = secureBytesGen,
    ): AESEncryption {
        val encryptionKeyRepository: EncryptionKeyRepository = mockk()

        if (throwError)
            coEvery { encryptionKeyRepository.getOrGenerateAesEncryptionSecretKey() } throws Exception("test")
        else {
            val secretKey = aesEncryptionKeyFactory.generateSecretKey(
                password = password,
                salt = salt,
                osApiLevel = 30
            )
            coEvery { encryptionKeyRepository.getOrGenerateAesEncryptionSecretKey() } returns secretKey
        }

        return AESEncryption(
            encryptionKeyRepository = encryptionKeyRepository,
            secureBytesGenerator = rng,
            dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(testDispatcher),
            base64 = base64
        )
    }

    test("when we encrypt a string then we get the same string back if we decrypt it") {
        // Arrange
        val aes = aes()
        val testString = "a test string"
        // Act
        val encryptedValue = aes.encrypt(testString)
        val decryptedValue = aes.decrypt(encryptedValue)
        // Assert
        encryptedValue shouldNotBe testString
        decryptedValue shouldBe testString
    }

    test("when we encrypt bytes then we get the same bytes back if we decrypt it") {
        // Arrange
        val aes = aes()
        val testBytes = Random.nextBytes(100)
        // Act
        val encryptedValue = aes.encrypt(testBytes)
        val decryptedValue = aes.decrypt(encryptedValue)
        // Assert
        encryptedValue shouldNotBe testBytes
        decryptedValue shouldBe testBytes
    }

    test("when we use wrong password then throw exception") {
        // Arrange
        val aesA = aes(password = "password1")
        val aesB = aes(password = "password2")
        val testBytes = Random.nextBytes(100)
        // Act
        try {
            aesA.encrypt(testBytes).let { aesB.decrypt(it) }
            fail("expected exception")
        } catch (ex: Exception) {
            // no-op
        }
    }

    class Function(val name: String, val block: suspend AESEncryption.(bytes: ByteArray) -> Unit)


    test("when we use wrong salt then throw exception") {
        // Arrange
        val aesA = aes(salt = secureBytesGen.nextBytes(8))
        val aesB = aes(salt = secureBytesGen.nextBytes(9))
        val testBytes = Random.nextBytes(100)
        // Act
        try {
            aesA.encrypt(testBytes).let { aesB.decrypt(it) }
            fail("expected exception")
        } catch (ex: Exception) {
            // no-op
        }
    }

    test("when we encrypt identical bytes twice then we get different encrypted values") {
        // Arrange
        val aes = aes()
        val testBytes = Random.nextBytes(100)
        // Act
        val encryptedValue1 = aes.encrypt(testBytes)
        val encryptedValue2 = aes.encrypt(testBytes)
        // Assert
        encryptedValue1 shouldNotBe encryptedValue2
    }

    test("when we encrypt identical bytes twice and nextBytes stays constant then we get a identical encrypted values") {
        // Arrange
        val aes = aes(rng = mockSecureBytesGen(secureBytesGen.nextBytes(16)))
        val testBytes = Random.nextBytes(100)
        // Act
        val encryptedValue1 = aes.encrypt(testBytes)
        val encryptedValue2 = aes.encrypt(testBytes)
        // Assert
        encryptedValue1 shouldBe encryptedValue2
    }

    listOf(Function("encrypt") { encrypt(it) }, Function("decrypt") { encrypt(it) }).forEach { function ->

        test("when exceptions is thrown during ${function.name} then it should be instance of EncryptionException") {
            // Arrange
            val aes = aes(throwError = true)
            val testBytes = Random.nextBytes(100)
            // Act
            try {
                function.block(aes, testBytes)
                fail("expected exception")
            } catch (ex: EncryptionException) {
                // no-op
            } catch (ex: Exception) {
                fail("exception must be wrapped in EncryptionException")
            }
        }
    }

    listOf(Function("encrypt") { encrypt(it) }, Function("decrypt") { encrypt(it) }).forEach { function ->
        test("when trying to ${function.name} empty bytes then throw exception") {
            // Arrange
            val aes = aes()
            val testBytes = byteArrayOf()
            // Act
            try {
                function.block(aes, testBytes)
                fail("expected exception")
            } catch (ex: EncryptionException) {
                // no-op
            } catch (ex: Exception) {
                fail("exception must be wrapped in EncryptionException")
            }
        }
    }

    test("when trying to decrypt empty content then throw exception") {
        // Arrange
        val aes = aes()
        val testBytes = AESEncryption.Parser().encode(AESEncryption.EncryptedBlob(Random.nextBytes(16), byteArrayOf()))
        // Act
        try {
            aes.decrypt(testBytes)
            fail("expected exception")
        } catch (ex: EncryptionException) {
            // no-op
        } catch (ex: Exception) {
            fail("exception must be wrapped in EncryptionException")
        }
    }
})