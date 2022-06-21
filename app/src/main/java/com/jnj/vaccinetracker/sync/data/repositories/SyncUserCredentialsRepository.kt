package com.jnj.vaccinetracker.sync.data.repositories

import com.jnj.vaccinetracker.common.data.encryption.SecretSharedPreferences
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.di.qualifiers.SyncApi
import com.jnj.vaccinetracker.common.exceptions.SyncUserCredentialsNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.isDebugMode
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for encrypted storage/retrieval of synchronization account credentials.
 */
class SyncUserCredentialsRepository @Inject constructor(
    private val secretSharedPreferences: SecretSharedPreferences,
    private val dispatchers: AppCoroutineDispatchers,
    @SyncApi
    private val cookieRepository: CookieRepository,
) {

    private companion object {
        const val PREF_SYNC_USERNAME = "syncUser"
        const val PREF_SYNC_PASSWORD = "syncPassword"
    }

    private val syncUsername get() = secretSharedPreferences.getString(PREF_SYNC_USERNAME)
    private val syncPassword get() = secretSharedPreferences.getString(PREF_SYNC_PASSWORD)

    fun areSyncUserCredentialsStored(): Boolean = getSyncUserCredentialsOrNull() != null

    fun getSyncUserCredentials(): SyncUserCredentials {
        return getSyncUserCredentialsOrNull() ?: throw SyncUserCredentialsNotAvailableException("couldn't find sync user credentials")
    }

    fun getSyncUserCredentialsOrNull(): SyncUserCredentials? {
        val username = syncUsername
        val password = syncPassword
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) return null
        return SyncUserCredentials(username, password)
    }

    suspend fun saveSyncUserCredentials(credentials: SyncUserCredentials) = withContext(dispatchers.io) {
        if (isDebugMode)
            logInfo("saveUserCredentials: ${credentials.username}")
        secretSharedPreferences.putString(PREF_SYNC_USERNAME, credentials.username)
        secretSharedPreferences.putString(PREF_SYNC_PASSWORD, credentials.password)
    }

    suspend fun deleteSyncUserCredentials() = withContext(dispatchers.io) {
        secretSharedPreferences.remove(PREF_SYNC_USERNAME)
        secretSharedPreferences.remove(PREF_SYNC_PASSWORD)
        cookieRepository.clearAll()
    }

    fun observeSyncUserCredentials(): Flow<SyncUserCredentials?> {
        return secretSharedPreferences.observeString(PREF_SYNC_USERNAME).combine(secretSharedPreferences.observeString(PREF_SYNC_PASSWORD)) { username, password ->
            if (username.isNullOrEmpty() || password.isNullOrEmpty()) null
            else
                SyncUserCredentials(username, password)
        }
    }
}