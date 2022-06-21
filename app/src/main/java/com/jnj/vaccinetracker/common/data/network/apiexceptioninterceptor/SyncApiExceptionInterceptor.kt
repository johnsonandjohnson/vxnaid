package com.jnj.vaccinetracker.common.data.network.apiexceptioninterceptor

import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.di.qualifiers.SyncApi
import com.jnj.vaccinetracker.common.exceptions.InvalidSessionException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncApiExceptionInterceptor @Inject constructor(
    @SyncApi
    val cookieRepository: CookieRepository,
    moshi: Moshi,
) : ApiExceptionInterceptorBase(moshi) {
    override fun onSessionExpired() {
        logInfo("onSessionExpired")
        cookieRepository.clearSessionCookieBlocking()
        throw InvalidSessionException()
    }
}