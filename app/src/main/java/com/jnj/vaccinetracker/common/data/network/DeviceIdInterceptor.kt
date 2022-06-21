package com.jnj.vaccinetracker.common.data.network

import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DeviceIdInterceptor @Inject constructor(private val userRepository: UserRepository) : Interceptor {

    companion object {
        const val DEVICE_ID_HEADER = "deviceId"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .header(DEVICE_ID_HEADER, userRepository.getDeviceGuid())
            .build()
        return chain.proceed(newRequest)
    }

}