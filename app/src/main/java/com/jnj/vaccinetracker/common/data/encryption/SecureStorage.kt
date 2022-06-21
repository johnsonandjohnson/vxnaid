package com.jnj.vaccinetracker.common.data.encryption

import android.content.SharedPreferences
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface SecureStorage {
    fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    suspend fun putStringNullable(key: String, value: String?)
    suspend fun remove(key: String)
    fun observeString(key: String): Flow<String?>
    fun entries(): Map<String, String?>
    fun dispose()
}

class SecureStorageThreadSafe(
    private val fileName: String,
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val dispatchers: AppCoroutineDispatchers,
) : SecureStorage {
    private val mutex = Mutex()

    private val prefs by lazy {
        encryptedSharedPreferencesFactory.createEncryptedPreferences(fileName)
    }

    private val keyFlow = MutableSharedFlow<String?>(extraBufferCapacity = 100)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> keyFlow.tryEmit(key) }

    init {
        initState()
    }

    private fun initState() {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun dispose() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun entries() = prefs.all.filterValues { it is String? }.mapValues { it.value as String? }

    override fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    override suspend fun putString(key: String, value: String) = putStringNullable(key, value)


    override suspend fun putStringNullable(key: String, value: String?) = withContext(dispatchers.io) {
        mutex.withLock {
            prefs.edit().putString(key, value).apply()
        }
    }

    override suspend fun remove(key: String) = withContext(dispatchers.io) {
        mutex.withLock {
            prefs.edit().remove(key).apply()
        }
    }

    override fun observeString(key: String): Flow<String?> = keyFlow
        .filter { it == key || it == null } // null means preferences were cleared (Android R+ exclusive behavior)
        .onStart { emit("first load trigger") }
        .map { getString(key) }
        .conflate()
}

