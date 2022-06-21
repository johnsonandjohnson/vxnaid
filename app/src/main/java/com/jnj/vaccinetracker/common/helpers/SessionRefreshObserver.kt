package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.qualifiers.MainApi
import com.jnj.vaccinetracker.common.domain.entities.OperatorCredentials
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class SessionRefreshObserver @Inject constructor(
    @MainApi
    private val cookieRepository: CookieRepository,
    private val operatorCredentialsRepository: OperatorCredentialsRepository,
    private val userRepository: UserRepository,
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val networkConnectivity: NetworkConnectivity,
) {

    private data class LocalUser(val username: String, val userCredentials: OperatorCredentials?)

    private fun observeLocalUser(): Flow<LocalUser?> {
        return userRepository.observeUsername().flatMapLatest { username ->
            if (username == null)
                flowOf(null)
            else
                operatorCredentialsRepository.observeUsername(username).map { credentials ->
                    LocalUser(username, credentials)
                }
        }.distinctUntilChanged()
    }

    val sessionRefreshEvents
        get() = combine(cookieRepository.observeValidSessionCookieExists(), observeLocalUser()) { sessionCookieExists, localUser ->
            localUser != null && when (networkConnectivity.isConnectedFast()) {
                true -> sessionCookieExists
                false -> localUser.userCredentials != null
            }
        }.onEach { hasValidSession ->
            if (!hasValidSession) {
                sessionExpiryObserver.notifySessionExpired()
            }
        }
}