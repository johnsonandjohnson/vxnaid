package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.models.api.response.UserRole
import com.jnj.vaccinetracker.common.data.models.api.response.isOperator
import com.jnj.vaccinetracker.common.data.models.api.response.isSyncAdmin
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.di.qualifiers.SyncApi
import com.jnj.vaccinetracker.common.exceptions.SyncAdminAuthenticationException
import com.jnj.vaccinetracker.common.exceptions.SyncUserCredentialsNotAvailableException
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAdminLoginUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    @SyncApi
    private val syncCookieRepository: CookieRepository,
) {

    private fun validateRoles(roles: List<UserRole>) {
        logInfo("validateRoles")
        when {
            !roles.isSyncAdmin() -> {
                logInfo("!roles.isSyncAdmin")
                throw SyncAdminAuthenticationException(reason = SyncAdminAuthenticationException.Reason.NotSyncAdminRole)
            }
            roles.isOperator() -> {
                logInfo("roles.isOperator")
                throw SyncAdminAuthenticationException(reason = SyncAdminAuthenticationException.Reason.OperatorRole)
            }
            else -> {
                logInfo("is sync admin and not operator")
            }
        }
    }

    private suspend fun onSuccessfulLogin(syncUserCredentials: SyncUserCredentials) {
        logInfo("onSuccessfulLogin: ${syncUserCredentials.username}")
        syncUserCredentialsRepository.saveSyncUserCredentials(syncUserCredentials)
    }

    private suspend fun onFailedLogin(syncUserCredentials: SyncUserCredentials, oldCredentials: SyncUserCredentials?) {
        logInfo("onFailedLogin: ${syncUserCredentials.username} old: ${oldCredentials?.username}")
        if (oldCredentials != null) {
            syncUserCredentialsRepository.saveSyncUserCredentials(oldCredentials)
        }
        syncCookieRepository.clearSessionCookie()
    }

    suspend fun login(syncUserCredentials: SyncUserCredentials) {
        val username = syncUserCredentials.username
        logInfo("login: $username")
        var oldCredentials: SyncUserCredentials? = null
        try {
            // Save old credentials and remove the existing ones
            oldCredentials = syncUserCredentialsRepository.getSyncUserCredentials()
            syncUserCredentialsRepository.deleteSyncUserCredentials()
        } catch (ex: SyncUserCredentialsNotAvailableException) {
            // No old credentials available
        }
        var successLogin = false
        try {
            val response = try {
                api.login(syncUserCredentials)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("login error", ex)
                throw SyncAdminAuthenticationException("error occurred during login", cause = ex, reason = SyncAdminAuthenticationException.Reason.RemoteLoginError)
            }
            if (response.sessionId == null || !response.authenticated || response.user == null)
                throw SyncAdminAuthenticationException("user '$username' not authenticated", reason = SyncAdminAuthenticationException.Reason.InvalidCredentials)

            validateRoles(response.user.roles)
            successLogin = true
        } finally {
            if (successLogin) {
                onSuccessfulLogin(syncUserCredentials)
            } else {
                onFailedLogin(syncUserCredentials = syncUserCredentials, oldCredentials = oldCredentials)
            }
        }
    }
}