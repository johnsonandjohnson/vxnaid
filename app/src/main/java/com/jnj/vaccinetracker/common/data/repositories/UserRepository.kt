package com.jnj.vaccinetracker.common.data.repositories

import com.jnj.vaccinetracker.common.di.qualifiers.MainApi
import com.jnj.vaccinetracker.common.domain.entities.User
import com.jnj.vaccinetracker.common.exceptions.DeviceNameNotAvailableException
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class UserRepository @Inject constructor(
    private val prefs: FlowSharedPreferences,
    @MainApi
    private val cookieRepository: CookieRepository,
) {

    private companion object {
        private const val PREF_USER_UUID = "uuid"
        private const val PREF_USER_DISPLAY = "display"
        private const val PREF_USER_USERNAME = "username"
        private const val PREF_LAST_USERNAME = "lastUsername"
        private const val PREF_DEVICE_GUID = "deviceGuid"
        private const val PREF_LOGIN_TIME = "loginTime"
        private const val PREF_DEVICE_NAME = "deviceName"
        private const val PREF_DEVICE_NAME_SITE_UUID = "deviceNameSiteUuid"
    }

    private val lastUsername get() = prefs.getNullableString(PREF_LAST_USERNAME)
    private val loginTime get() = prefs.getLong(PREF_LOGIN_TIME)
    private val userUuid get() = prefs.getNullableString(PREF_USER_UUID)
    private val userDisplay get() = prefs.getNullableString(PREF_USER_DISPLAY)
    private val userUsername get() = prefs.getNullableString(PREF_USER_USERNAME)
    private val deviceGuid get() = prefs.getNullableString(PREF_DEVICE_GUID)
    private val deviceName get() = prefs.getNullableString(PREF_DEVICE_NAME)
    private val deviceNameSiteUuid get() = prefs.getNullableString(PREF_DEVICE_NAME_SITE_UUID)


    fun getUser(): User? {
        val uuid = userUuid.get() ?: return null
        val display = userDisplay.get() ?: return null
        val username = userUsername.get() ?: return null
        return User(uuid, display, username)
    }

    fun getLoginTime(): Date? {
        return loginTime.get().takeIf { it > 0L }?.let { Date(it) }
    }

    private fun genAndSetDeviceId(): String {
        val newGuid = UUID.randomUUID().toString()
        deviceGuid.set(newGuid)
        return newGuid
    }

    fun getDeviceGuid(): String {
        return deviceGuid.get() ?: genAndSetDeviceId()
    }

    fun getDeviceNameSiteUuid(): String? {
        return deviceNameSiteUuid.get()
    }

    fun getDeviceName(): String? {
        return deviceName.get()
    }

    fun getDeviceNameOrThrow(): String {
        return deviceName.get() ?: throw DeviceNameNotAvailableException()
    }

    fun clearDeviceName() {
        deviceName.delete()
        deviceNameSiteUuid.delete()
    }

    fun saveUser(user: User, loginTime: Date) {
        userUuid.set(user.uuid)
        userDisplay.set(user.display)
        userUsername.set(user.username)
        lastUsername.set(user.username)
        this.loginTime.set(loginTime.time)
    }


    fun deleteUser() {
        userDisplay.delete()
        userUuid.delete()
        userUsername.delete()
        loginTime.delete()
    }

    fun setNewDeviceName(name: String, siteUuid: String) {
        deviceName.set(name)
        deviceNameSiteUuid.set(siteUuid)
    }

    fun logOut() {
        cookieRepository.clearAll()
        deleteUser()
    }

    fun observeUsername() = userUsername.asFlow()

    fun observeLastUsername() = lastUsername.asFlow()

    fun observeDeviceName(): Flow<String?> = deviceName.asFlow()

    fun observeUserDisplay(): Flow<String?> = userDisplay.asFlow()

    fun isLoggedIn(): Boolean = getUser() != null
}