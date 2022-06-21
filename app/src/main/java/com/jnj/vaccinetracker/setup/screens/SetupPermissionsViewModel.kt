package com.jnj.vaccinetracker.setup.screens

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.setup.helpers.Permissions
import javax.inject.Inject

class SetupPermissionsViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val permissions: Permissions,
) : ViewModelBase() {
    val permissionsSettingsCompleted = eventFlow<Unit>()
    val loading = mutableLiveBoolean()
    val permissionDoze = mutableLiveBoolean()
    val permissionCamera = mutableLiveBoolean()
    val permissionInstall = mutableLiveBoolean()
    val showPermissionInstall = mutableLiveBoolean()
    val showPermissionDoze = mutableLiveBoolean()
    val canDone = mutableLiveBoolean()

    init {
        initState()
    }

    private fun initState() {
        // Only show permission for installing apps if the app is being deployed manually
        // and the Android API version is 26 or higher
        if (permissions.packageInstallPermissionRequired) {
            showPermissionInstall.set(true)
            checkInstallPermissionSet()
        }
        // Only show permissions for doze mode if the Android API version is 23 or higher
        if (permissions.dosePermissionSupported) {
            showPermissionDoze.set(true)
            checkDozePermissionSet()
        }
        checkCameraPermissionSet()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        logInfo("updateButtonStates")
        canDone.value = permissions.areMandatoryPermissionsSet()
    }

    /**
     * Check all permissions and adjust the indications accordingly
     */
    fun checkAllPermissionStatus() {
        checkInstallPermissionSet()
        checkDozePermissionSet()
        checkCameraPermissionSet()
        updateButtonStates()
    }

    /**
     * Verify if the permission to ignore battery optimizations is set
     * and set the [permissionDoze] mutableLiveBoolean accordingly.
     * Will always be set to true if API level is lower than 23 (as no doze mode in older versions).
     */
    private fun checkDozePermissionSet() {
        permissionDoze.value = permissions.dosePermissionSet
    }

    /**
     * Verify if the permission to allow camera access is set
     * and set the [permissionCamera] mutableLiveBoolean accordingly.
     */
    private fun checkCameraPermissionSet() {
        permissionCamera.value = permissions.cameraPermissionSet
    }

    /**
     * Verify if the permission to request installing packages is set
     * and set the [permissionInstall] mutableLiveBoolean accordingly.
     * Will always be set to true if API level is lower than 26 (as no per-app setting in older versions).
     */
    private fun checkInstallPermissionSet() {
        permissionInstall.value = permissions.packageInstallPermissionsSet
    }

    /**
     * Check if mandatory permissions are set to complete this setup step.
     * If true, will emit on the [permissionsSettingsCompleted] eventFlow.
     */
    fun onDoneClick() {
        logInfo("onDoneClick: ${canDone.value}")
        if (canDone.value) {
            permissionsSettingsCompleted.tryEmit(Unit)
        }
    }

    fun setCameraPermissionGranted() {
        checkCameraPermissionSet()
        updateButtonStates()
    }
}