package com.jnj.vaccinetracker.setup

import android.os.Bundle
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.setup.models.P2pDeviceRole
import com.jnj.vaccinetracker.setup.models.SetupScreen
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import javax.inject.Inject

class SetupFlowViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsObserver: SyncSettingsObserver,
) : ViewModelWithState() {
    val loading = mutableLiveBoolean()
    val currentScreen = mutableLiveData<SetupScreen>(SetupScreen.Intro)
    var navigationDirection = NavigationDirection.NONE
    val showCancelButton = mutableLiveBoolean()
    var deviceType = mutableLiveData<P2pDeviceRole>()
    val showConfirmBackPressDialog = eventFlow<Unit>()

    private companion object {
        private const val STATE_CURRENT_SCREEN = "current_screen"
        private const val STATE_DEVICE_TYPE = "device_type"
    }

    init {
        initState()
    }

    private fun initState() {
        // Show cancel button only if the sync settings were already set
        val syncSettingsAvailable = syncSettingsObserver.isSyncSettingsAvailable()
        showCancelButton.set(syncSettingsAvailable)
    }

    private fun <T> List<T>.nextAt(item: T) = indexOf(item).takeIf { it != -1 }?.let { it + 1 }?.let { i -> if (i in indices) this[i] else null }
    private fun <T> List<T>.previousAt(item: T) = (indexOf(item) - 1).let { i -> if (i in indices) this[i] else null }

    /**
     * Navigate back in the screen order
     *
     * @return True if a previous screen exists, false otherwise.
     */
    fun navigateBack(forced: Boolean = false): Boolean {
        logInfo("navigateBack: $forced")
        val currentScreen = currentScreen.get()
        if (currentScreen == null) {
            logError("currentScreen == null")
            return false
        }
        if (!forced && currentScreen == SetupScreen.MainMenu.Item.P2pSync.Transfer) {
            logInfo("confirming back press")
            showConfirmBackPressDialog.tryEmit(Unit)
            return false
        }
        var previousScreen = SetupScreen.values().previousAt(currentScreen)
        if (previousScreen == null) {
            previousScreen = SetupScreen.MainMenu.Item.P2pSync.values().previousAt(currentScreen)
        }
        if (previousScreen == null && currentScreen is SetupScreen.MainMenu.Item) {
            previousScreen = SetupScreen.MainMenu.Menu
        }

        if (previousScreen != null) {
            navigationDirection = NavigationDirection.BACKWARD
            this.currentScreen.set(previousScreen)
            return true
        }
        logError("no previous screen available")
        return false
    }

    /**
     * Navigate forward in the screen order
     *
     * @return True if a next screen exists, false otherwise.
     */
    private fun navigateForward(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        var nextScreen = SetupScreen.values().nextAt(currentScreen)
        if (nextScreen == null) {
            nextScreen = SetupScreen.MainMenu.Item.P2pSync.values().nextAt(currentScreen)
        }
        if (nextScreen != null) {
            navigationDirection = NavigationDirection.FORWARD
            this.currentScreen.set(nextScreen)
            return true
        }

        return false
    }

    private fun navigateToMenu() {
        navigationDirection = NavigationDirection.BACKWARD
        this.currentScreen.set(SetupScreen.MainMenu.Menu)
    }

    fun confirmMenuItem() {
        navigateForward()
    }

    /**
     * Execute on completion of intro screen
     */
    fun startSetup() {
        navigateForward()
    }

    /**
     * Execute on completion of backend settings screen
     */
    fun confirmBackendSetup() {
        navigateForward()
    }

    /**
     * Execute on completion of backend settings screen
     */
    fun confirmSiteSetup() {
        navigateBack()
    }

    fun confirmLicensesSetup() {
        navigateBack()
    }

    fun confirmPermissions() {
        navigateBack()
    }

    fun onP2pTransferCompleted() {
        navigateToMenu()
    }

    fun confirmStopP2PTransfer() {
        navigateToMenu()
    }

    fun confirmP2PDeviceType(p2pDeviceRole: P2pDeviceRole) {
        this.deviceType.value = p2pDeviceRole
        navigateForward()
    }

    fun openMenuItem(itemScreen: SetupScreen.MainMenu.Item) {
        navigationDirection = NavigationDirection.FORWARD
        this.currentScreen.set(itemScreen)
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_CURRENT_SCREEN, currentScreen.get())
        outState.putSerializable(STATE_DEVICE_TYPE, deviceType.value)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        currentScreen.set(savedInstanceState.getParcelable(STATE_CURRENT_SCREEN))
        deviceType.set(savedInstanceState.getSerializable(STATE_DEVICE_TYPE) as? P2pDeviceRole)
    }

}