package com.jnj.vaccinetracker.sync.domain.usecases.download

import com.jnj.vaccinetracker.common.data.encryption.SecureStorage
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import kotlinx.coroutines.flow.*

class FakeCookieRepository(override val dispatchers: AppCoroutineDispatchers) : CookieRepository() {
    override fun createSecureStorage(): SecureStorage {
        return InMemorySecureStorage()
    }

}

class InMemorySecureStorage : SecureStorage {
    private val map = mutableMapOf<String, String?>()
    private val changed = MutableSharedFlow<String>(0, 100)
    override fun getString(key: String): String? {
        return map[key]
    }

    override suspend fun putString(key: String, value: String) {
        putStringNullable(key, value)
    }

    override suspend fun putStringNullable(key: String, value: String?) {
        map[key] = value
        changed.tryEmit(key)
    }

    override suspend fun remove(key: String) {
        map.remove(key)
        changed.tryEmit(key)
    }

    override fun observeString(key: String): Flow<String?> {
        return changed.filter { it == key }
            .onStart { emit("first load trigger") }
            .map { getString(key) }
            .conflate()
    }

    override fun entries(): Map<String, String?> {
        return map.toMap()
    }

    override fun dispose() {
        /*
        Empty override.
         */
    }

}