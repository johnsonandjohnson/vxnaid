package com.jnj.vaccinetracker.common.data.repositories

import com.jnj.vaccinetracker.common.data.encryption.EncryptedSharedPreferencesFactory
import com.jnj.vaccinetracker.common.data.encryption.SecureStorage
import com.jnj.vaccinetracker.common.data.encryption.SecureStorageThreadSafe
import com.jnj.vaccinetracker.common.data.models.SerializableCookie
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logDebug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author druelens
 * @version 1
 */
abstract class CookieRepository : CookieJar {
    private val secureStorage by lazy {
        createSecureStorage()
    }
    protected abstract val dispatchers: AppCoroutineDispatchers

    protected abstract fun createSecureStorage(): SecureStorage

    private companion object {
        private const val PREF_COOKIE_PREFIX = "VMPcookie_"
        private const val SESSION_COOKIE_NAME = "JSESSIONID"

        private const val SESSION_COOKIE_PREF_KEY = PREF_COOKIE_PREFIX + SESSION_COOKIE_NAME


        private fun SecureStorage.cookies() = entries().filterKeys { it.contains(PREF_COOKIE_PREFIX) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {

        val cookies: MutableList<Cookie> = ArrayList()

        for ((key, value) in secureStorage.cookies()) {
            if (value != null) {
                val cookie = SerializableCookie().decode(value)
                if (cookie != null) {
                    if (cookie.isExpired()) {
                        clearCookie(key)
                    } else {
                        logDebug("loading cookie: " + cookie.name)
                        logDebug("expires at: " + cookie.expiresAt.toString())
                        logDebug("current time: " + System.currentTimeMillis().toString())
                        cookies.add(cookie)
                    }
                }
            }
        }
        return cookies

    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) {
            logDebug("saving cookie: " + c.name)
            runBlocking {
                secureStorage.putStringNullable(createCookieKey(c), SerializableCookie().encode(c))
            }
        }
    }

    private fun createCookieKey(cookie: Cookie): String {
        return PREF_COOKIE_PREFIX + cookie.name
    }

    private fun Cookie.isValid() = !isExpired()


    private fun Cookie.isExpired() = expiresAt < System.currentTimeMillis()

    private fun clearCookie(key: String) = runBlocking {
        secureStorage.remove(key)
    }

    fun clearAll() = runBlocking {
        val keysToRemove = secureStorage.cookies().keys
        for (key in keysToRemove) {
            secureStorage.remove(key)
        }
    }

    suspend fun clearSessionCookie() {
        secureStorage.remove(SESSION_COOKIE_PREF_KEY)
    }

    fun clearSessionCookieBlocking() = runBlocking {
        clearSessionCookie()
    }

    private fun String.toCookie() = SerializableCookie().decode(this)

    fun observeValidSessionCookieExists(): Flow<Boolean> {
        return secureStorage.observeString(SESSION_COOKIE_PREF_KEY)
            .map {
                it?.toCookie()?.isValid() == true
            }
            .distinctUntilChanged()
    }

    /**
     * Check if a non-expired session cookie exists.
     *
     * @return True if it exists and is not expired. False otherwise.
     */
    fun validSessionCookieExists(): Boolean {
        return secureStorage.getString(SESSION_COOKIE_PREF_KEY)?.toCookie()?.isValid() == true
    }

}

@Singleton
class SyncCookieRepository @Inject constructor(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    override val dispatchers: AppCoroutineDispatchers,
) : CookieRepository() {
    companion object {
        private const val COOKIE_JAR_FILE_NAME = "sync_cookie_prefs"
    }

    override fun createSecureStorage(): SecureStorage {
        return SecureStorageThreadSafe(COOKIE_JAR_FILE_NAME, encryptedSharedPreferencesFactory, dispatchers)
    }
}

@Singleton
class MainCookieRepository @Inject constructor(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    override val dispatchers: AppCoroutineDispatchers,
) : CookieRepository() {
    companion object {
        private const val COOKIE_JAR_FILE_NAME = "main_cookie_prefs"
    }

    override fun createSecureStorage(): SecureStorage {
        return SecureStorageThreadSafe(COOKIE_JAR_FILE_NAME, encryptedSharedPreferencesFactory, dispatchers)
    }
}