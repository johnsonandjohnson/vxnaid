package com.jnj.vaccinetracker.sync.p2p.domain.services

import com.jnj.vaccinetracker.common.data.encryption.PasswordHasher
import com.jnj.vaccinetracker.common.data.encryption.SecureBytesGenerator
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice
import com.jnj.vaccinetracker.sync.p2p.common.models.LoginResult
import com.jnj.vaccinetracker.sync.p2p.common.models.LoginStatus
import com.jnj.vaccinetracker.sync.p2p.common.models.NsdSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class P2pLoginService @Inject constructor(
    private val passwordHasher: PasswordHasher,
    private val dispatchers: AppCoroutineDispatchers,
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    private val secureBytesGenerator: SecureBytesGenerator,
    private val base64: Base64,
) {

    companion object {
        private const val SESSION_LEN = 32
    }

    private val _session = MutableStateFlow<NsdSession?>(null)
    val session: StateFlow<NsdSession?> get() = _session

    fun getSession(nsdDevice: CompatibleNsdDevice): NsdSession? {
        return session.value?.takeIf { it.device == nsdDevice }
    }

    fun clear() {
        _session.value = null
    }

    private suspend fun createSessionToken(): String {
        return secureBytesGenerator.nextBytes(SESSION_LEN).let { base64.encode(it) }
    }

    suspend fun login(
        username: String,
        passwordHash: String,
        device: CompatibleNsdDevice,
    ): LoginResult {
        return withContext(dispatchers.io) {
            val isVerified = syncUserCredentialsRepository.getSyncUserCredentials()
                .let { passwordHasher.verify(password = it.password, passwordHash) }
            val loginStatus = when (isVerified) {
                true -> LoginStatus.AUTHENTICATED
                false -> LoginStatus.UNAUTHENTICATED
            }

            val sessionToken = when (loginStatus) {
                LoginStatus.AUTHENTICATED -> {
                    val session = NsdSession(
                        sessionToken = createSessionToken(),
                        device = device, username = username
                    )
                    _session.value = session
                    session.sessionToken
                }
                LoginStatus.UNAUTHENTICATED -> {
                    null
                }
            }
            LoginResult(sessionToken, loginStatus)
        }
    }
}