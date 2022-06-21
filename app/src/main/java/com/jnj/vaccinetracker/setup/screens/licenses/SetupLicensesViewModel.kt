package com.jnj.vaccinetracker.setup.screens.licenses

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.exceptions.LicensesNotObtainedException
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.config.appSettings
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SetupLicensesViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val licenseManager: LicenseManager,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {
    val activeLicenses = mutableLiveData<String>()
    val canDeactivateLicenses = mutableLiveBoolean(false)
    val canActivateLicenses = mutableLiveBoolean(false)
    val errorMessage = mutableLiveData<String>()
    val loading = mutableLiveBoolean()

    val finishScreenEvent = eventFlow<Unit>()

    init {
        initState()
    }

    private fun initState() {
        showActivatedLicenses()
    }

    private fun showActivatedLicenses() {
        val activatedLicenseTypes = licenseManager.getActivatedLicenseTypes()
        val licensesString: String
        if (activatedLicenseTypes.isEmpty()) {
            licensesString = resourcesWrapper.getString(R.string.settings_label_no_licenses)
            canDeactivateLicenses.set(false)
        } else {
            canDeactivateLicenses.set(true)
            licensesString = activatedLicenseTypes.joinToString(", ") { it.name }
        }
        activeLicenses.set(licensesString)
        canActivateLicenses.value = appSettings.showManualLicenseActivationButton && activatedLicenseTypes.size != LicenseType.values().size

        if (canActivateLicenses.value && canDeactivateLicenses.value && errorMessage.value == null) {
            errorMessage.set(resourcesWrapper.getString(R.string.settings_msg_error_cannot_activate_some_licenses))
        }
    }

    private fun isZeroLicensesActivated() = licenseManager.getActivatedLicenseTypes().isEmpty()

    @OptIn(DelicateCoroutinesApi::class)
    fun activateLicenses() {
        // we use global scope so the license activation can continue after dialog is closed.
        GlobalScope.launch {
            try {
                loading.set(true)
                errorMessage.set(null)
                licenseManager.getLicensesOrThrow(fromUserInput = true, licenseTypes = LicenseType.values().toList())
            } catch (ex: NoNetworkException) {
                errorMessage.set(resourcesWrapper.getString(R.string.settings_msg_no_network_cannot_activate))
            } catch (ex: LicensesNotObtainedException) {
                logError("activateLicenses error", ex)
                when {
                    ex.isObtainableAfterForceClose -> errorMessage.set(resourcesWrapper.getString(R.string.msg_no_iris_license_must_force_close))
                    isZeroLicensesActivated() -> errorMessage.set(resourcesWrapper.getString(R.string.settings_msg_error_cannot_activate))
                    else -> errorMessage.set(resourcesWrapper.getString(R.string.settings_msg_error_cannot_activate_some_licenses))
                }
            } finally {
                showActivatedLicenses()
                loading.set(false)
            }

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun deactivateLicenses() {
        GlobalScope.launch {
            try {
                loading.set(true)
                errorMessage.set(null)
                licenseManager.deactivateLicenses()
            } catch (ex: NoNetworkException) {
                errorMessage.set(resourcesWrapper.getString(R.string.settings_msg_no_network_cannot_deactive))
            } catch (ex: Exception) {
                logError("deactivateLicenses error", ex)
                errorMessage.set(resourcesWrapper.getString(R.string.settings_msg_error_cannot_deactivate))
            } finally {
                showActivatedLicenses()
                loading.set(false)
            }

        }
    }

    fun onFinishButtonClick() {
        finishScreenEvent.tryEmit(Unit)
    }
}