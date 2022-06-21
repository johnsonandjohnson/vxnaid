package com.jnj.vaccinetracker.setup.screens.mainmenu

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.setup.helpers.Permissions
import com.jnj.vaccinetracker.setup.models.SetupMenuItem
import com.jnj.vaccinetracker.setup.models.SetupMenuItemUiModel
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SetupMainMenuViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val licenseManager: LicenseManager,
    private val syncSettingsObserver: SyncSettingsObserver,
    private val permissions: Permissions,
    private val resourcesWrapper: ResourcesWrapper
) : ViewModelBase() {


    val menuItems = MutableStateFlow<List<SetupMenuItemUiModel>>(emptyList())
    val isFinishButtonEnabled = mutableLiveBoolean()
    val setupCompletedEvent = eventFlow<Unit>()
    val errorMessage = mutableLiveData<String?>()
    val openMenuItemEvent = eventFlow<SetupMenuItem>()

    init {
        initMenu()
    }

    private fun SetupMenuItem.calcShowCheckmark(): Boolean {
        return when (this) {
            SetupMenuItem.P2P_TRANSFER -> false
            else -> true
        }
    }

    private fun SetupMenuItem.calcIsDone(): Boolean {
        return when (this) {
            SetupMenuItem.P2P_TRANSFER -> true
            SetupMenuItem.LICENSES -> licenseManager.getActivatedLicenseTypes().containsAll(LicenseType.values().toList())
            SetupMenuItem.SITE -> syncSettingsObserver.isSiteSelectionAvailable()
            SetupMenuItem.PERMISSIONS -> permissions.areMandatoryPermissionsSet()
        }
    }

    private fun initMenu() {
        scope.launch {
            refreshMenu()
        }
    }

    private fun refreshMenu() {
        menuItems.value = SetupMenuItem.values().map { item ->
            SetupMenuItemUiModel(item, item.calcIsDone(), item.calcShowCheckmark())
        }
        updateButtonStates()
    }

    private val isSiteSelected get() = menuItems.value.find { it.setupMenuItem == SetupMenuItem.SITE }?.isDone == true

    private fun calcCanProceed(): Boolean {
        val mandatoryPermissionsDone = menuItems.value.find { it.setupMenuItem == SetupMenuItem.PERMISSIONS }?.isDone == true
        return mandatoryPermissionsDone && isSiteSelected
    }

    private fun updateButtonStates() {
        isFinishButtonEnabled.value = calcCanProceed()
    }

    fun onMenuItemClick(menuItem: SetupMenuItemUiModel) {
        errorMessage.value = null
        if (menuItem.setupMenuItem == SetupMenuItem.P2P_TRANSFER && !isSiteSelected) {
            errorMessage.value = resourcesWrapper.getString(R.string.setup_main_menu_error_no_site_selected)
            return
        }
        openMenuItemEvent.tryEmit(menuItem.setupMenuItem)
    }

    fun onFinishClick() {
        if (isFinishButtonEnabled.value) {
            setupCompletedEvent.tryEmit(Unit)
        }
    }
}