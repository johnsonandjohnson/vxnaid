package com.jnj.vaccinetracker.common.data.helpers

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.util.Base64 as AndroidBase64

interface Base64 {
    suspend fun encodeString(string: String): String
    suspend fun encode(byteArray: ByteArray): String
    suspend fun decodeToString(base64: String): String
    suspend fun decode(base64: String): ByteArray

    companion object {
        operator fun invoke() = Base64Impl(AppCoroutineDispatchers.DEFAULT)
    }
}

class Base64Impl @Inject constructor(private val dispatchers: AppCoroutineDispatchers) : Base64 {
    private val flags = AndroidBase64.DEFAULT
    private val coroutineContext get() = dispatchers.io
    override suspend fun encodeString(string: String): String = withContext(coroutineContext) {
        encode(string.toByteArray())
    }

    override suspend fun encode(byteArray: ByteArray): String = withContext(coroutineContext) {
        AndroidBase64.encodeToString(byteArray, flags)
    }

    override suspend fun decodeToString(base64: String): String = withContext(coroutineContext) {
        String(decode(base64))
    }

    override suspend fun decode(base64: String): ByteArray = withContext(coroutineContext) {
        AndroidBase64.decode(base64, flags)
    }
}