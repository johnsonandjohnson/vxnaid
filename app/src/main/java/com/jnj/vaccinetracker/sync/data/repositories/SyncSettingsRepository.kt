package com.jnj.vaccinetracker.sync.data.repositories

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.datasources.SyncCompletedDateDataSource
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.tfcporciuncula.flow.FlowSharedPreferences
import javax.inject.Inject

/**
 * Repository for settings related to synchronization of data between backend and Android client.
 */
class SyncSettingsRepository @Inject constructor(
    private val prefs: FlowSharedPreferences,
    private val resourcesWrapper: ResourcesWrapper,
    private val masterDataRepository: MasterDataRepository,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
    private val syncCompletedDateDataSource: SyncCompletedDateDataSource,
) {

    private companion object {
        const val PREF_BACKEND_URL = "backendUrl"
        private const val PREF_SITE_UUID = "siteUuid"
    }

    private val backendUrlNullable get() = prefs.getNullableString(PREF_BACKEND_URL, null)
    private val backendUrl get() = prefs.getString(PREF_BACKEND_URL, resourcesWrapper.getString(R.string.default_base_url))
    private val siteUuid get() = prefs.getNullableString(PREF_SITE_UUID)

    fun saveBackendUrl(backendUrl: String) {
        if (backendUrl != this.backendUrl.get()) {
            logWarn("saveBackendUrl and deleting master data + siteUuid")
            siteUuid.delete()
            masterDataMemoryDataSource.clear()
            masterDataRepository.deleteAll()
        }
        this.backendUrl.set(backendUrl)
    }

    fun saveSiteUuid(siteUuid: String) {
        if (siteUuid != this.siteUuid.get()) {
            syncCompletedDateDataSource.syncParticipantDateCompletedPref.delete()
        }
        this.siteUuid.set(siteUuid)
    }

    fun observeBackendUrl() = backendUrl.asFlow()
    fun observeSiteUuid() = siteUuid.asFlow()

    fun getBackendUrl(): String = backendUrl.get()
    fun getBackendUrlOrNull(): String? = backendUrlNullable.get()
    fun getSiteUuid(): String? = siteUuid.get()
    fun getSiteUuidOrThrow(): String = getSiteUuid() ?: throw NoSiteUuidAvailableException()
}