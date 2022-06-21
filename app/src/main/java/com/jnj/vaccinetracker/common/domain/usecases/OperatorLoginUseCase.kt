package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.jnj.vaccinetracker.common.data.encryption.PasswordHasher
import com.jnj.vaccinetracker.common.data.models.api.response.*
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSource
import com.jnj.vaccinetracker.common.domain.entities.OperatorCredentials
import com.jnj.vaccinetracker.common.domain.entities.User
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.exceptions.OperatorAuthenticationException
import com.jnj.vaccinetracker.common.exceptions.SyncUserCredentialsNotAvailableException
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperatorLoginUseCase @Inject constructor(
    private val operatorCredentialsRepository: OperatorCredentialsRepository,
    private val api: VaccineTrackerApiDataSource,
    private val passwordHasher: PasswordHasher,
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
) {

    private fun throwIfCachedSyncAdmin(username: String, password: String) {
        try {
            val credentials = syncUserCredentialsRepository.getSyncUserCredentials()
            if (credentials.let { it.username.equals(username, ignoreCase = true) && it.password == password }) {
                throw createSyncAdminError()
            }
        } catch (ex: SyncUserCredentialsNotAvailableException) {
            return
        }
    }

    private fun createSyncAdminError() = OperatorAuthenticationException("Sync admin not allowed to login as operator",
        reason = OperatorAuthenticationException.Reason.SyncAdminRole)


    private suspend fun loginOffline(username: String, password: String): OperatorCredentials {
        val operator = operatorCredentialsRepository.findByUsername(username)
        fun createError(reason: OperatorAuthenticationException.Reason) = OperatorAuthenticationException("invalid credentials for user '$username'", reason = reason)
        if (operator != null) {
            val isVerified = passwordHasher.verify(password = password, bcryptHash = operator.passwordHash)
            if (isVerified) {
                return operator
            } else {
                throw createError(OperatorAuthenticationException.Reason.LocalCredentialsPasswordMismatch)
            }
        } else {
            throw createError(OperatorAuthenticationException.Reason.LocalCredentialsNotFound)
        }

    }

    private suspend fun onLoggedInOnline(user: UserDto, password: String) {
        val passwordHash = passwordHasher.hash(password)
        val credentials = OperatorCredentials.fromUser(user = user.toDomain(), passwordHash = passwordHash)
        try {
            operatorCredentialsRepository.insert(credentials, orReplace = true)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("Failed to insert operator credentials", ex)
        }

    }

    private fun validateRoles(roles: List<UserRole>) {
        logInfo("validateRoles")
        when {
            roles.isSyncAdmin() -> {
                logInfo("roles.isSyncAdmin")
                throw createSyncAdminError()
            }
            !roles.isOperator() -> {
                logInfo("!roles.isOperator")
                throw OperatorAuthenticationException("You must be an Operator to login here.",
                    reason = OperatorAuthenticationException.Reason.NotOperatorRole)
            }
            else -> {
                logInfo("is not sync admin and is operator")
            }
        }
    }

    suspend fun login(username: String, password: String): User {
        logInfo("login: $username")
        throwIfCachedSyncAdmin(username, password)
        val response = try {
            api.login(username, password)
        } catch (ex: NoNetworkException) {
            return loginOffline(username, password).user
        }
        if (response.sessionId == null || !response.authenticated || response.user == null)
            throw OperatorAuthenticationException("user '$username' not authenticated", reason = OperatorAuthenticationException.Reason.RemoteLoginError)

        validateRoles(response.user.roles)

        onLoggedInOnline(response.user, password)

        return response.user.toDomain()
    }
}