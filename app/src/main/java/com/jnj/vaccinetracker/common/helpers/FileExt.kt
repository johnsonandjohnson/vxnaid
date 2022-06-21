package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.data.helpers.Md5HashGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

suspend fun File.readBytesAsync(context: CoroutineContext = Dispatchers.IO): ByteArray = withContext(context) {
    readBytes()
}

suspend fun File.readBytesOrNull(context: CoroutineContext = Dispatchers.IO): ByteArray? {
    return try {
        readBytesAsync(context)
    } catch (ex: Exception) {
        yield()
        ex.rethrowIfFatal()
        logError("readBytesOrNull failed", ex)
        null
    }
}

fun File.toTemp() = File(parentFile, "$name.tmp")

fun File.deleteChildren(): Boolean {
    val folder = this
    return walkTopDown().fold(null as Boolean?, { res, it ->
        if (res == null) {
            require(folder == it) { "folder ${folder.name} is not equal to first file ${it.name}" }
            true
        } else ((it.delete() || !it.exists()) && res)
    }) ?: true
}

private const val MINIMUM_BLOCK_SIZE: Int = 512

suspend fun File.md5Hash(dispatchers: AppCoroutineDispatchers = AppCoroutineDispatchers.DEFAULT) =
    Md5HashGenerator(dispatchers).md5(this)

private fun FileInputStream.skipAccurate(skip: Long, retryCount: Int = 1): FileInputStream = apply {
    if (skip != 0L) {
        val bytesSkipped = skip(skip)
        val difference = bytesSkipped - skip
        if (difference != 0L) {
            if (retryCount > 0)
                skipAccurate(difference, retryCount - 1)
            else {
                throw IOException("can't skip file exactly to $skip bytes. difference=$difference bytes")
            }
        }
    }
}

private fun InputStream.closeSilently() = use { }

private fun File.inputStreamStartingFrom(start: Long): InputStreamWithProgress {
    val input = inputStream()
    return try {
        input.also {
            it.skipAccurate(start)
        }.let { InputStreamWithProgress(it, start) }
    } catch (e: IOException) {
        logError("failed to skip bytes: $start", e)
        // close current inputStream and open a new one
        input.closeSilently()
        InputStreamWithProgress(inputStream(), 0)
    }
}

private class InputStreamWithProgress(val inputStream: FileInputStream, val progress: Long)

/**
 * Reads file by byte blocks and calls [action] for each block read.
 * This functions passes the byte array and amount of bytes in the array to the [action] function.
 *
 * You can use this function for huge files.
 *
 * @param action function to process file blocks. return false if you don't want to continue
 * @param blockSize size of a block, replaced by 512 if it's less
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun File.forEachBlockAsync(
    blockSize: Int,
    skip: Long = 0,
    action: suspend (buffer: ByteArray, bytesRead: Int, progress: Long, max: Long, isFirstBytes: Boolean) -> Boolean,
) = withContext(Dispatchers.IO) {
    val arr = ByteArray(blockSize.coerceAtLeast(MINIMUM_BLOCK_SIZE))
    val max = length()
    val inputStreamWithProgress = inputStreamStartingFrom(skip)
    inputStreamWithProgress.inputStream.use { input ->
        var progress = inputStreamWithProgress.progress
        do {
            val size = input.read(arr)
            if (size <= 0) {
                break
            } else {
                val isFirstBytes = progress == 0L
                progress += size
                val buff = if (size != arr.size) {
                    arr.copyOf(size)
                } else arr
                if (!action(buff, size, progress, max, isFirstBytes))
                    break
            }
        } while (coroutineContext.isActive)
    }
}