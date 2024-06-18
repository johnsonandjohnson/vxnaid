package com.jnj.vaccinetracker.register.screens

import androidx.collection.ArrayMap
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.yearNow
import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.GenerateUniqueParticipantIdUseCase
import com.jnj.vaccinetracker.common.domain.usecases.GetTempBiometricsTemplatesBytesUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.exceptions.ParticipantAlreadyExistsException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.model.DisplayValue
import com.jnj.vaccinetracker.common.validators.ParticipantIdValidator
import com.jnj.vaccinetracker.common.validators.PhoneValidator
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toDomain
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@SuppressWarnings("TooManyFunctions")
class RegisterParticipantParticipantDetailsViewModel @Inject constructor(
        private val phoneValidator: PhoneValidator,
        private val syncSettingsRepository: SyncSettingsRepository,
        private val configurationManager: ConfigurationManager,
        private val resourcesWrapper: ResourcesWrapper,
        private val participantManager: ParticipantManager,
        override val dispatchers: AppCoroutineDispatchers,
        private val participantIdValidator: ParticipantIdValidator,
        private val sessionExpiryObserver: SessionExpiryObserver,
        private val getTempBiometricsTemplatesBytesUseCase: GetTempBiometricsTemplatesBytesUseCase,
        private val fullPhoneFormatter: FullPhoneFormatter,
        private val generateUniqueParticipantIdUseCase: GenerateUniqueParticipantIdUseCase,
) : ViewModelBase() {

    private companion object {
        private const val YEAR_OF_BIRTH_MIN_VALUE = 1900
        private val YEAR_OF_BIRTH_MAX_VALUE = yearNow()
        private const val YEAR_OF_BIRTH_LENGTH = 4

        /**
         * wait this long before we validate a field while typing
         */
        private val INLINE_VALIDATION_DELAY = 2.seconds

    }

    data class Args(
            val participantId: String?,
            val isManualSetParticipantID: Boolean,
            val leftEyeScanned: Boolean,
            val rightEyeScanned: Boolean,
            val phoneNumber: String?,
    )

    private val args = stateFlow<Args?>(null)

    val registerSuccessEvents = eventFlow<ParticipantSummaryUiModel>()
    val registerFailedEvents = eventFlow<String>()
    val registerNoPhoneEvents = eventFlow<Unit>()
    val registerNoMatchingIdEvents = eventFlow<Unit>()

    val loading = mutableLiveBoolean()
    val participantId = mutableLiveData<String?>()
    val scannedParticipantId = mutableLiveData<String?>()
    val confirmParticipantId = mutableLiveData<String?>()
    val confirmParticipantIdVisibility = mutableLiveBoolean()
    val isManualSetParticipantID = mutableLiveBoolean()
    val isAutoGeneratedParticipantId = mutableLiveBoolean()

    val nin = mutableLiveData<String?>()
    val ninValidationMessage = mutableLiveData<String>()

    val name = mutableLiveData<String>()
    val nameValidationMessage = mutableLiveData<String>()

    val mothersName = mutableLiveData<String>()
    val mothersNameValidationMessage = mutableLiveData<String>()

    val birthWeight = mutableLiveData<String>()
    val birthWeightValidationMessage = mutableLiveData<String>()

    val fathersName = mutableLiveData<String>()
    val fathersNameValidationMessage = mutableLiveData<String>()

    val childCategory = mutableLiveData<DisplayValue>()
    val childCategoryValidationMessage = mutableLiveData<String>()
    val childCategoryNames = mutableLiveData<List<DisplayValue>>()

    val birthDate = mutableLiveData<DateTime>()
    val birthDateText = mutableLiveData<String>()
    val birthDateValidationMessage = mutableLiveData<String>()

    val isBirthDateEstimated = mutableLiveData<Boolean>()

    val leftIrisScanned = mutableLiveBoolean()
    val rightIrisScanned = mutableLiveBoolean()
    val gender = mutableLiveData<Gender>()
    val defaultPhoneCountryCode = mutableLiveData<String>()
    private val phoneCountryCode = mutableLiveData<String>()
    val phone = mutableLiveData<String>()
    val homeLocationLabel = mutableLiveData<String>()
    private val homeLocation = mutableLiveData<Address>()
    val vaccine = mutableLiveData<DisplayValue>()
    val language = mutableLiveData<DisplayValue>()

    val participantIdValidationMessage = mutableLiveData<String>()
    val confirmParticipantIdValidationMessage = mutableLiveData<String>()

    val genderValidationMessage = mutableLiveData<String>()
    val phoneValidationMessage = mutableLiveData<String>()
    val homeLocationValidationMessage = mutableLiveData<String>()
    val languageValidationMessage = mutableLiveData<String>()

    val vaccineNames = mutableLiveData<List<DisplayValue>>()
    val languages = mutableLiveData<List<DisplayValue>>()

    var canSkipPhone = false
    private val irisScans = ArrayMap<IrisPosition, Boolean>()

    private var validatePhoneJob: Job? = null
    private var validateParticipantIdJob: Job? = null

    init {
        initState()
    }

    private suspend fun load(args: Args) {
        loading.set(true)
        try {
            val config = configurationManager.getConfiguration()
            isAutoGeneratedParticipantId.value = config.isAutoGenerateParticipantId
            if (config.isAutoGenerateParticipantId) {
                isManualSetParticipantID.value = false
                participantId.value = generateUniqueParticipantIdUseCase.generateUniqueParticipantId()
                confirmParticipantId.value = participantId.value
            } else {
                isManualSetParticipantID.value = args.isManualSetParticipantID
                participantId.value = args.participantId
                if (!isManualSetParticipantID.value) {
                    confirmParticipantId.value = participantId.value
                    scannedParticipantId.value = participantId.value
                }
            }
            confirmParticipantIdVisibility.set(isManualSetParticipantID.get())
            leftIrisScanned.set(args.leftEyeScanned)
            rightIrisScanned.set(args.rightEyeScanned)
            irisScans[IrisPosition.LEFT] = args.leftEyeScanned
            irisScans[IrisPosition.RIGHT] = args.rightEyeScanned

            args.phoneNumber?.let {
                phone.set(it)
            }

            val site = syncSettingsRepository.getSiteUuid()?.let { configurationManager.getSiteByUuid(it) }
                    ?: throw NoSiteUuidAvailableException()
            val configuration = configurationManager.getConfiguration()
            val loc = configurationManager.getLocalization()
            onSiteAndConfigurationLoaded(site, configuration, loc)
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            logError("Failed to get site by uuid: ", ex)
        }
    }

    private fun initState() {
        args.filterNotNull().distinctUntilChanged()
                .onEach { args ->
                    load(args)
                }.launchIn(scope)
    }

    fun setArguments(args: Args) {
        this.args.tryEmit(args)
    }

    private fun onSiteAndConfigurationLoaded(site: Site, configuration: Configuration, loc: TranslationMap) {
        defaultPhoneCountryCode.set(site.countryCode)
        if (phoneCountryCode.get() == null) phoneCountryCode.set(site.countryCode)

        vaccineNames.set(configuration.vaccines.map { vaccine -> vaccine.name }.map { DisplayValue(it, loc[it]) })
        // TODO: Load up correct keys
        childCategoryNames.set(arrayOf("National", "Foreigner", "Refugee").map { DisplayValue(it, loc[it]) })
        languages.set(configuration.personLanguages.map { language -> language.name }.map { DisplayValue(it, loc[it]) })
    }

    private suspend fun ImageBytes.compress() = ImageHelper.compressRawImage(this, dispatchers.io)

    @SuppressWarnings("LongParameterList", "LongMethod")
    fun submitRegistration(
            picture: ParticipantImageUiModel?,
    ) {
        scope.launch {
            doRegistration(picture)
        }
    }

    private suspend fun doRegistration(
            picture: ParticipantImageUiModel?,
    ) {
        val siteUuid = syncSettingsRepository.getSiteUuid()
                ?: return logWarn("Cannot submit registration: no site UUID known")
        val homeLocation = homeLocation.get()
        val participantId = participantId.get()
        val nin = nin.get()
        logInfo("setting up birthweight")
        val birthWeight = birthWeight.get()
        logInfo("setting up birthweight")
        val gender = gender.get()
        val birthDate = birthDate.get()
        val isBirthDateEstimated = isBirthDateEstimated.get()
        val fullPhoneNumber = createFullPhone()

        val isValidInput = validateInput(participantId, gender, birthDate, homeLocation)

        var phoneNumberToSubmit: String? = null

        //if manual entered participantId check if it is matching incoming one
        if (isManualSetParticipantID.get() && confirmParticipantId.value != participantId) {
            logInfo("participantId: $participantId")
            logInfo("confirmParticipantId: ${confirmParticipantId.value}")
            registerNoMatchingIdEvents.tryEmit(Unit)
            return
        }

        // Validate the phone number input. If empty, it shows the dialog that it can be skipped.
        if (isValidInput && phone.get().isNullOrEmpty() && !canSkipPhone) {
            registerNoPhoneEvents.tryEmit(Unit)
            return
        } else if (!phone.get().isNullOrEmpty() && !phoneValidator.validate(fullPhoneNumber)) {
            phoneValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_phone))
            return
        } else if (!phone.get().isNullOrEmpty()) {
            phoneNumberToSubmit = fullPhoneNumber
        }
        if (!isValidInput)
            return

        loading.set(true)

        val loc = configurationManager.getLocalization()
        try {
            val compressedImage = picture?.toDomain()?.compress()
            val biometricsTemplateBytes = getTempBiometricsTemplatesBytesUseCase.getBiometricsTemplate(irisScans)
            val result = participantManager.registerParticipant(

                participantId = participantId!!,
                nin = nin,
                birthWeight = birthWeight,
                gender = gender!!,
                birthDate = birthDate!!,
                isBirthDateEstimated = isBirthDateEstimated!!,
                telephone = phoneNumberToSubmit,
                siteUuid = siteUuid,
                language = "English",
                vaccine = vaccine?.value!!,
                address = homeLocation!!,
                picture = compressedImage,
                biometricsTemplateBytes = biometricsTemplateBytes,

                    participantId = participantId!!,
                    nin = nin,
                    gender = gender!!,
                    birthDate = birthDate!!,
                    isBirthDateEstimated = isBirthDateEstimated!!,
                    telephone = phoneNumberToSubmit,
                    siteUuid = siteUuid,
                    language = "English",
                    address = homeLocation!!,
                    picture = compressedImage,
                    biometricsTemplateBytes = biometricsTemplateBytes,

            )
            loading.set(false)
            registerSuccessEvents.tryEmit(
                    ParticipantSummaryUiModel(
                            result.participantUuid,
                            participantId,
                            gender,
                            birthDate.format(DateFormat.FORMAT_DATE),
                            isBirthDateEstimated,
                            null,
                            compressedImage?.let { ParticipantImageUiModel(it.bytes) }
                    )
            )
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            logError("Failed to register participant: ", ex)
            when (ex) {
                is ParticipantAlreadyExistsException -> {
                    val errorMessage = resourcesWrapper.getString(R.string.participant_registration_details_error_participant_already_exists)
                    participantIdValidationMessage.set(errorMessage)
                    registerFailedEvents.tryEmit(errorMessage)
                }

                is OperatorUuidNotAvailableException -> {
                    sessionExpiryObserver.notifySessionExpired()
                }

                else -> {
                    registerFailedEvents.tryEmit(resourcesWrapper.getString(R.string.general_label_error))
                }
            }

        }
    }

    @SuppressWarnings("LongParameterList")
    private suspend fun validateInput(
            participantId: String?,
            gender: Gender?,
            birthDate: DateTime?,
            homeLocation: Address?,
    ): Boolean {
        var isValid = true
        resetValidationMessages()

        if (participantId.isNullOrEmpty()) {
            isValid = false
            participantIdValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_participant_id))
        } else if (!participantIdValidator.validate(participantId)) {
            isValid = false
            participantIdValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_invalid_participant_id))
        }

        if (gender == null) {
            isValid = false
            genderValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_gender))
        }

        if (birthWeight == null ){
            isValid = false
            birthWeightValidationMessage.set("Please enter birth weight as integer")
        }

        if (homeLocation?.isEmpty() != false) {
            isValid = false
            homeLocationValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_home_location))
        }

        if (birthDate == null) {
            isValid = false
            birthDateValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_birthday))
        }

        return isValid
    }

    private fun resetValidationMessages() {
        participantIdValidationMessage.set(null)
        confirmParticipantIdValidationMessage.set(null)
        ninValidationMessage.set(null)
        genderValidationMessage.set(null)
        birthWeightValidationMessage.set(null)
        birthDateValidationMessage.set(null)
        phoneValidationMessage.set(null)
        homeLocationValidationMessage.set(null)
        languageValidationMessage.set(null)
    }

    fun setGender(gender: Gender) {
        if (this.gender.get() == gender) return
        this.gender.set(gender)
        genderValidationMessage.set(null)
    }

    fun onParticipantIdScanned(participantIdBarcode: String) {
        logInfo("onParticipantIdScanned: $participantIdBarcode")
        isManualSetParticipantID.set(false)
        scannedParticipantId.set(participantIdBarcode)
        setConfirmParticipantId(participantIdBarcode)
        setParticipantId(participantIdBarcode)
    }

    fun setParticipantId(participantId: String) {
        if (this.participantId.get() == participantId) return
        this.participantId.set(participantId)


        //if we change the id, and this does not match the confirmed id anymore, we need to confirm
        if (confirmParticipantId.value != this.participantId.value) {
            confirmParticipantIdVisibility.set(true)
            isManualSetParticipantID.set(true)
            if (!isManualSetParticipantID.value) {
                confirmParticipantId.set(null)
            }
        }

        //in case we have a scanned id, and we change our id back to original scanned value, the confirm field disappears
        if (participantId == scannedParticipantId.value) {
            confirmParticipantIdVisibility.set(false)
            isManualSetParticipantID.set(false)
            setConfirmParticipantId(participantId)
        }
        validateParticipantId()
    }

    fun setConfirmParticipantId(participantId: String) {
        if (this.confirmParticipantId.get() == participantId) return
        this.confirmParticipantId.set(participantId)
        validateParticipantId()
    }

    fun setNin(nin: String) {
        if (this.nin.get() == nin) return
        this.nin.set(nin)
        // TODO: Validate NIN
    }

    fun setBirthWeight(birthWeight: String) {
        if(this.birthWeight.get() == birthWeight) return
        this.birthWeight.set(birthWeight)
    }

    private fun validateParticipantId() {
        logInfo("validateParticipantId")
        participantIdValidationMessage.set(null)
        confirmParticipantIdValidationMessage.set(null)
        validateParticipantIdJob?.cancel()
        validateParticipantIdJob = scope.launch {
            delaySafe(INLINE_VALIDATION_DELAY)
            val validateParticipantId = participantId.value
            if (!validateParticipantId.isNullOrEmpty() && !participantIdValidator.validate(validateParticipantId)) {
                participantIdValidationMessage.value = resourcesWrapper.getString(R.string.participant_registration_details_error_invalid_participant_id)
            }
            if (isManualSetParticipantID.value) {
                logDebug("confirm participant id value: ${confirmParticipantId.value}")
                //We do not set the error on initial loading of screen hence the !=null check
                if (confirmParticipantId.value != participantId.value && !confirmParticipantId.value.isNullOrEmpty()) {
                    confirmParticipantIdValidationMessage.value = resourcesWrapper.getString(R.string.participant_registration_details_error_not_same_participant_id)
                }
            }
        }
    }

    fun setBirthDate(birthDate: DateTime, isChecked: Boolean) {
        val currentBirthDate = this.birthDate.get()
        val currentIsChecked = this.isBirthDateEstimated.get()

        if (currentBirthDate == birthDate && currentIsChecked == isChecked) return

        this.birthDate.set(birthDate)
        val formattedDate = birthDate.format(DateFormat.FORMAT_DATE)
        this.birthDateText.set(formattedDate)
        birthDateValidationMessage.set(null)
        isBirthDateEstimated.set(isChecked)
    }

    private fun createFullPhone(): String {
        val phone = phone.value ?: return ""
        val phoneCountryCode = phoneCountryCode.get() ?: return ""
        return fullPhoneFormatter.toFullPhoneNumberOrNull(phone, phoneCountryCode) ?: ""
    }


    private fun validatePhone() {
        logInfo("validatePhone")
        phoneValidationMessage.set(null)
        validatePhoneJob?.cancel()
        validatePhoneJob = scope.launch {
            delaySafe(INLINE_VALIDATION_DELAY)
            val fullPhone = createFullPhone()
            if (fullPhone.isNotEmpty() && !phoneValidator.validate(fullPhone)) {
                phoneValidationMessage.set(resourcesWrapper.getString(R.string.participant_registration_details_error_no_phone))
            }
        }
    }

    fun setPhone(phone: String) {
        if (this.phone.get() == phone) return
        this.phone.set(phone)
        validatePhone()
    }

    fun setPhoneCountryCode(selectedCountryCode: String) {
        if (phoneCountryCode.get() == selectedCountryCode) return // Break feedback loop
        phoneCountryCode.set(selectedCountryCode)
        validatePhone()
    }

    fun setSelectedChildCategory(childCategoryName: DisplayValue) {
        if (this.childCategory.get() == childCategoryName) return
        childCategory.set(childCategoryName)
        childCategoryValidationMessage.set(null)
    }

    fun setHomeLocation(homeLocation: Address, stringRepresentation: String) {
        this.homeLocation.set(homeLocation)
        this.homeLocationLabel.set(stringRepresentation)
        homeLocationValidationMessage.set(null)
    }
}
