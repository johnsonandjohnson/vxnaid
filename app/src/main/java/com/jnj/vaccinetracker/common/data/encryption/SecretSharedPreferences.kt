package com.jnj.vaccinetracker.common.data.encryption

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecretSharedPreferences @Inject constructor(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val dispatchers: AppCoroutineDispatchers,
) : SecureStorage by SecureStorageThreadSafe(SECRET_SHARED_PREFERENCES_NAME, encryptedSharedPreferencesFactory, dispatchers) {

    companion object {
        private const val SECRET_SHARED_PREFERENCES_NAME = "secret_shared_prefs"
    }
}