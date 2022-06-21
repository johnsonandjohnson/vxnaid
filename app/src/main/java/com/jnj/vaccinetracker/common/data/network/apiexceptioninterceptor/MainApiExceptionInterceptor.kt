package com.jnj.vaccinetracker.common.data.network.apiexceptioninterceptor

import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.exceptions.InvalidSessionException
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainApiExceptionInterceptor @Inject constructor(
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val userRepository: UserRepository,
    moshi: Moshi,
) : ApiExceptionInterceptorBase(moshi) {

    override fun onSessionExpired() {
        logInfo("onSessionExpired")
        sessionExpiryObserver.notifySessionExpired()
        userRepository.logOut()
        throw InvalidSessionException()
    }
}