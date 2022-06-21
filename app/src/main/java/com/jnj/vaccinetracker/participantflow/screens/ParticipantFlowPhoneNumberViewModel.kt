package com.jnj.vaccinetracker.participantflow.screens

import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.validators.PhoneValidator
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

class ParticipantFlowPhoneNumberViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val phoneValidator: PhoneValidator,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val configurationManager: ConfigurationManager,
    private val prefs: FlowSharedPreferences,
) : ViewModelBase() {

    val canSubmit = mutableLiveBoolean()
    val canSkip = mutableLiveBoolean()
    val loading = mutableLiveBoolean()

    val defaultPhoneCountryCode = mutableLiveData<String>()
    val phoneCountryCode = mutableLiveData<String>()
    val phone = mutableLiveData<String>()
    val showNoPhoneWarningEvent = eventFlow<Unit>()
    val confirmWithNullValuesEvent = eventFlow<Unit>()
    val prefCountryCode get() = prefs.getNullableString(PREF_COUNTRY_CODE)

    companion object {
        private const val PREF_COUNTRY_CODE = "countryCode"
    }


    init {
        scope.launch {
            loading.set(true)
            try {
                val site = syncSettingsRepository.getSiteUuid()?.let { configurationManager.getSiteByUuid(it) } ?: throw NoSiteUuidAvailableException()
                logInfo("site country ${site.countryCode} ${site.country}")
                defaultPhoneCountryCode.set(prefCountryCode.get())
                prefCountryCode.get()?: defaultPhoneCountryCode.set(site.countryCode)
                loading.set(false)
            } catch (ex: Throwable) {
                yield()
                ex.rethrowIfFatal()
                loading.set(false)
                logError("Failed to get site by uuid: ", ex)
            }
        }
    }

    fun onSkipButtonClick() {
        scope.launch {
            confirmWithNullValuesEvent.tryEmit(Unit)
        }
    }

    fun onConfirmNoPhoneClick() {
        confirmWithNullValuesEvent.tryEmit(Unit)
    }

    fun setPhoneCountryCode(selectedCountryCode: String) {
        if (phoneCountryCode.get() == selectedCountryCode) return // Break feedback loop
        phoneCountryCode.set(selectedCountryCode)
    }

    fun setPrefCountryCode(selectedCountryCode: String) {
        if (prefCountryCode.get() == selectedCountryCode) return // Break feedback loop
        prefCountryCode.set(selectedCountryCode)
    }

    fun validateInput(phone: String?) {
        val fullPhoneNumber = "${phoneCountryCode.get()}${phone}".replace(" ", "")

        // Validate the phone number input. If empty, it shows the dialog that it can be skipped.
        if (phone.isNullOrEmpty() || !phoneValidator.validate(fullPhoneNumber)) {
            canSubmit.set(false)
            return
        } else {
            this.phone.set(phone)
            canSubmit.set(true)
        }
    }

}