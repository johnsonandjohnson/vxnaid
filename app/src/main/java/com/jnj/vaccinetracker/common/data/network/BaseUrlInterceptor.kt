package com.jnj.vaccinetracker.common.data.network

import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(private val syncSettingsRepository: SyncSettingsRepository) : Interceptor {

    companion object {
        const val BASE_URL = "https://base-url-to-be-replaced.com"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        val newRequest = if (url.startsWith(BASE_URL)) {
            val newUrl = url.replace(BASE_URL, syncSettingsRepository.getBackendUrl()).toHttpUrl()
            request.newBuilder()
                .url(newUrl)
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }

}