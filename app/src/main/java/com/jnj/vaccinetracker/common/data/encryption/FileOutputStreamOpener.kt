package com.jnj.vaccinetracker.common.data.encryption

import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.seconds
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import javax.inject.Inject

class FileOutputStreamOpener @Inject constructor() {

    companion object {
        private val DEFAULT_DELAY = 3.seconds
        private const val DEFAULT_RETRY_COUNT = 3
    }

    suspend fun openOutputStream(file: File, retryCount: Int = DEFAULT_RETRY_COUNT, delayMs: Long = DEFAULT_DELAY): OutputStream {
        require(delayMs >= 0L) { "delayMs must be positive or zero" }
        require(retryCount >= 0) { "retryCount must be positive or zero" }
        return try {
            file.outputStream()
        } catch (ex: FileNotFoundException) {
            if (retryCount == 0) {
                logError("openOutputStream ${file.name} retryCount zero, rethrowing ex...")
                throw ex
            }
            logError("openOutputStream ${file.name} retry ($retryCount) after $delayMs")
            if (delayMs > 0L) {
                delay(delayMs)
            }
            openOutputStream(file, retryCount - 1, delayMs)
        }
    }
}