package com.jnj.vaccinetracker.common.data.helpers

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import okio.HashingSource
import okio.source
import okio.use
import java.io.File
import javax.inject.Inject

class Md5HashGenerator @Inject constructor(private val dispatchers: AppCoroutineDispatchers) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun md5(bigFile: File): String = withContext(dispatchers.io) {
        logInfo("md5: ${bigFile.name}")
        val dateModifiedOriginal = bigFile.lastModified()
        val hash = withContext(dispatchers.computation) {
            HashingSource.md5(bigFile.source()).use { it.hash }.hex()
        }
        if (dateModifiedOriginal == bigFile.lastModified()) {
            hash
        } else {
            md5(bigFile)
        }
    }


    suspend fun md5(input: ByteArray): String = withContext(dispatchers.io) {
        input.toByteString().md5().hex()
    }


    suspend fun md5(input: String): String = md5(input.toByteArray())
}