@file:Suppress("BlockingMethodInNonBlockingContext")

package com.jnj.vaccinetracker.common.data.encryption

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class EncryptionService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val aesEncryption: AESEncryption,
    private val fileOutputStreamOpener: FileOutputStreamOpener,
) {

    private fun File.openFileInputStream(): InputStream {
        return inputStream()
    }

    private suspend fun File.openFileOutputStream(): OutputStream {
        return fileOutputStreamOpener.openOutputStream(this)
    }

    /**
     * encrypt given [byteArray] and write to given [f]
     */
    suspend fun writeEncryptedFile(f: File, byteArray: ByteArray, isAlreadyEncrypted: Boolean): Unit =
        withContext(dispatchers.io) {
            val encryptedBytes = if (isAlreadyEncrypted) byteArray else aesEncryption.encrypt(byteArray)
            f.parentFile?.mkdirs()
            f.openFileOutputStream().buffered().use { outputStream ->
                outputStream.write(encryptedBytes)
                outputStream.flush()
            }
        }


    /**
     * encrypt given [text] and write to given [f]
     */
    suspend fun writeEncryptedFile(f: File, text: String): Unit =
        withContext(dispatchers.io) {
            val encryptedText = aesEncryption.encrypt(text)
            f.openFileOutputStream().bufferedWriter().use { outputStream ->
                outputStream.write(encryptedText)
                outputStream.flush()
            }
        }

    suspend fun readEncryptedFile(f: File): ByteArray = withContext(dispatchers.io) {
        f.openFileInputStream().use { it.readBytes() }.let { aesEncryption.decrypt(it) }
    }

    suspend fun readEncryptedFileAsText(f: File): String = withContext(dispatchers.io) {
        f.openFileInputStream().bufferedReader().use { it.readText() }.let { aesEncryption.decrypt(it) }
    }
}

