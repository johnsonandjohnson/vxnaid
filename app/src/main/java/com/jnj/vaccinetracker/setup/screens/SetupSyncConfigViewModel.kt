package com.jnj.vaccinetracker.setup.screens

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.ui.model.SiteUiModel
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.yield
import javax.inject.Inject

class SetupSyncConfigViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val configurationManager: ConfigurationManager,
    private val resourcesWrapper: ResourcesWrapper,
    private val syncLogger: SyncLogger,
) : ViewModelBase() {
    val siteValidationMessage = mutableLiveData<String>()

    val syncSettingsCompleted = eventFlow<Unit>()

    val loading = mutableLiveBoolean()
    val selectedSite = mutableLiveData<SiteUiModel?>()
    val sites = mutableLiveData<List<SiteUiModel>>()
    var siteDropdownText: String? = null

    init {
        initState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initState() {
        // Load sites for dropdown
        syncLogger.observeMasterDataLoadedInMemory(MasterDataFile.SITES)
            .onStart { emit(Unit) }
            .mapLatest {
                loadSites()
            }.launchIn(scope)
    }

    private fun calcSite(text: String): SiteUiModel? {
        return if (text.isEmpty()) {
            null
        } else {
            sites.value?.find { it.displayName == text }
        }
    }

    fun onSiteTextChanged(text: String) {
        siteDropdownText = text
        val currentSite = selectedSite.value
        val site = calcSite(text)
        if (site != null) {
            if (currentSite != site)
                setSelectedSite(site)
        } else if (text.isEmpty()) {
            clearSite()
        } else {
            logInfo("No valid site found with provided text: $text")
        }
    }

    /**
     * Load sites to populate the site dropdown
     */
    private suspend fun loadSites() {
        logInfo("loadSites")
        try {
            loading.set(true)
            val configSites = configurationManager.getSites()
            val loc = configurationManager.getLocalization()
            loading.set(false)
            val uiModels = configSites.map { SiteUiModel.create(it, loc) }
            sites.value = uiModels
            if (selectedSite.value == null) {
                selectedSite.value = syncSettingsRepository.getSiteUuid()?.let { siteUuid ->
                    uiModels.find { it.uuid == siteUuid }
                }
            }
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            logError("Failed to get sites: ", throwable)
            loading.set(false)
        }
    }

    private fun clearSite() {
        selectedSite.value = null
    }

    /**
     * Set selected site
     */
    fun setSelectedSite(site: SiteUiModel) {
        selectedSite.set(site)
        siteValidationMessage.set(null)
    }

    /**
     * Validate if the settings in this fragment were entered correctly:
     * - Site was selected
     *
     * @return True if validation passed, false otherwise.
     */
    private fun validateSettings(site: SiteUiModel?): Boolean {
        var validated = true
        // Check if site is selected
        if (site == null || site.displayName != siteDropdownText) {
            siteValidationMessage.set(resourcesWrapper.getString(R.string.site_selection_error_no_site_selected))
            validated = false
        }
        return validated
    }

    /**
     * Try to save settings entered in this fragment if they pass validation.
     * Emits an event when successfully saved.
     */
    fun saveSyncSettings() {
        val site = selectedSite.get()

        if (!validateSettings(site)) {
            return
        }

        if (site != null) {
            syncSettingsRepository.saveSiteUuid(site.uuid)
        }
        syncSettingsCompleted.tryEmit(Unit)

    }
}