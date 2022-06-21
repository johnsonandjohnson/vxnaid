package com.jnj.vaccinetracker.participantflow

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.UpdateManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.IdentificationStep
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.model.SiteUiModel
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.neurotec.biometrics.NSubject
import com.neurotec.io.NFile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Responsible for managing the participant flow: gathers information about the patient in a couple of steps and ultimately searches patients with those data.
 */
class ParticipantFlowViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val configurationManager: ConfigurationManager,
    private val resourcesWrapper: ResourcesWrapper,
    private val androidFiles: AndroidFiles,
    override val dispatchers: AppCoroutineDispatchers,
    private val versionUpdateObserver: VersionUpdateObserver,
    private val updateManager: UpdateManager,
    private val fullPhoneFormatter: FullPhoneFormatter,
) : ViewModelWithState() {

    private companion object {
        private const val STATE_SCREENS = "screens"
        private const val STATE_CURRENT_SCREEN = "currentScreen"
        private const val STATE_PARTICIPANT_ID = "participantId"
        private const val STATE_PHONE_COUNTRY_CODE = "phoneCountryCode"
        private const val STATE_PHONE_NUMBER = "participantPhoneNumber"
    }

    private val retryClickEvents = eventFlow<Unit>()
    val updateAvailableEvents = eventFlow<Unit>()

    val currentScreen = mutableLiveData<Screen>()
    val loading = mutableLiveBoolean()
    val site = mutableLiveData<SiteUiModel>()
    val operator = mutableLiveData<String>()
    val errorMessage = mutableLiveData<String>()

    //id variables
    val participantId = mutableLiveData<String>()
    private val barcodeParticipantId = mutableLiveData<String>()
    val isManualSetParticipantId = mutableLiveData<Boolean>()
    var navigationDirection = NavigationDirection.NONE

    // Phone variables
    val phoneCountryCode = mutableLiveData<String>()
    val participantPhone = mutableLiveData<String>()
    // Note that for matching, you need the fullPhoneNumber in format: "${phoneCountryCode.get()}${participantPhone.get()}".replace(" ", "")

    // Iris scan variables
    val irisScans = ArrayMap<IrisPosition, Boolean>()
    val irisIndexes = ArrayMap<IrisPosition, Int?>()
    val subject = NSubject()

    // Screens used in identification process
    private var screens = listOf<Screen>()

    // Identification steps listed on home screen
    var workflowItems = listOf<WorkflowItem>()

    private val tempTemplateFile by lazy { File(androidFiles.cacheDir, Constants.IRIS_TEMPLATE_NAME) }

    init {
        initState()
    }

    //observe inside own coroutineScope so it can reload on specific events such as retry click
    private suspend fun load() {
        try {
            val authenticationSteps = configurationManager.getIdentificationSteps()
            val siteUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException()
            val siteUiModel = configurationManager.getSiteUiModelByUuid(siteUuid)
            logInfo("loaded sites & authenticationSteps")
            site.set(siteUiModel)
            screens = createScreens(authenticationSteps)
            workflowItems = createWorkflowItems(authenticationSteps)
            if (currentScreen.get() == null) {
                val screen = screens.firstOrNull()
                println("currentScreen is null, setting new screen to $screen")
                currentScreen.set(screen)
            }
            loading.set(false)
        } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            loading.set(false)
            errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            logError("Failed to load site list: ", throwable)
        }
    }

    /**
     * should only ever be called ONCE!
     */
    private fun initState() {
        userRepository.observeUserDisplay().onEach { display ->
            operator.value = display
        }.launchIn(scope)
        retryClickEvents.asFlow().mapLatest {
            errorMessage.set(null)
            loading.set(true)
            load()
        }.launchIn(scope)


        if (isManualFlavor) {
            updateManager.checkLatestVersion()
            versionUpdateObserver.versionUpdateEvents.onEach {
                updateAvailableEvents.tryEmit(Unit)
            }.launchIn(scope)
        }

        scope.launch {
            // emit event to trigger initial load
            retryClickEvents.tryEmit(Unit)
        }
        // make sure template is deleted on init
        tempTemplateFile.delete()
    }

    fun onRetryClick() {
        errorMessage.set(null)
        loading.set(true)
        retryClickEvents.tryEmit(Unit)
    }

    fun getFullPhoneNumber(): String? = fullPhoneFormatter.toFullPhoneNumberOrNull(
        phone = participantPhone.value.orEmpty(),
        countryCode = phoneCountryCode.value.orEmpty()
    )

    /**
     * Sets up the list of screens based upon the backend configuration of which authentication steps are available.
     * The flow always consists of
     * 1. Intro screen
     * 2. One or more authentication steps (e.g. entry of participant ID, iris scan, ...)
     * 3. Matching of participants based upon the entered data
     */
    private fun createScreens(identificationSteps: List<IdentificationStep>): List<Screen> {
        val result = mutableListOf(Screen.INTRO)

        identificationSteps.forEach { step ->
            when (step.type) {
                WorkflowItem.ID_CARD.type -> result += listOf(Screen.PARTICIPANT_ID)
                WorkflowItem.PHONE.type -> result += listOf(Screen.PHONE)
                WorkflowItem.IRIS_SCAN.type -> result += listOf(
                    Screen.IRIS_SCAN_RIGHT_EYE,
                    Screen.IRIS_SCAN_LEFT_EYE
                )
            }
        }

        result += Screen.PARTICIPANT_MATCHING
        return result
    }

    /**
     * Sets up the list of identification steps and their position based on backend configuration.
     * The flow always consists of
     * - One or more identification steps (e.g. entry of participant ID, iris scan, ...)
     * - Matching of participants based upon the entered data
     * - Visit follow-up actions
     */
    private fun createWorkflowItems(identificationSteps: List<IdentificationStep>): List<WorkflowItem> {
        val workflowItems = mutableListOf<WorkflowItem>()
        identificationSteps.forEach { step ->
            when (step.type) {
                WorkflowItem.ID_CARD.type -> {
                    WorkflowItem.ID_CARD.mandatory = step.mandatory
                    workflowItems.add(WorkflowItem.ID_CARD)
                }
                WorkflowItem.PHONE.type -> {
                    WorkflowItem.PHONE.mandatory = step.mandatory
                    workflowItems.add(WorkflowItem.PHONE)
                }
                WorkflowItem.IRIS_SCAN.type -> {
                    WorkflowItem.IRIS_SCAN.mandatory = step.mandatory
                    workflowItems.add(WorkflowItem.IRIS_SCAN)
                }
            }
        }
        workflowItems.add(WorkflowItem.MATCHING)
        workflowItems.add(WorkflowItem.VISIT)

        return workflowItems
    }

    /**
     * Confirms the first step (the intro). Requires no input from the user so it just proceeds to the next step
     */
    fun confirmIntro() {
        navigateForward()
    }

    fun setAutoGeneratedParticipantId(participantId: String) {
        logInfo("setAutoGeneratedParticipantId: $participantId")
        this.participantId.value = participantId
        isManualSetParticipantId.value = false
    }

    /**
     * Confirms the participant ID (castorId), which will be null when the user chose to skip this step,
     * or it was not included in the identification steps
     * manual indicates if the id was entered manually (true) or via barcode scanner (false)
     */
    fun confirmParticipantId(participantId: String?, manual: Boolean) {
        this.participantId.set(participantId)
        //if is scanned with barcode, set variable
        if (!manual) {
            this.barcodeParticipantId.set(participantId)
        }

        // Happens in case of going back to id screen when previously scanned with barcode. If code remains unchanged it is not set manually
        if (!barcodeParticipantId.value.isNullOrEmpty() && barcodeParticipantId.value == participantId) {
            this.isManualSetParticipantId.set(false)
        } else {
            this.isManualSetParticipantId.set(manual)

        }

        navigateForward()
    }

    /**
     * Confirms the participant phone number, which will be null when the user chose to skip this step,
     * or it was not included in the identification steps
     */
    fun confirmPhone(countryCode: String?, phoneNumber: String?) {
        this.phoneCountryCode.set(countryCode)
        this.participantPhone.set(phoneNumber)
        navigateForward()
    }

    /**
     * Confirms the Iris scan, when skipped the scan for this position will be discarded (if existing)
     *
     * @param eye                   IrisPosition at which the iris was scanned
     * @param scanned               Boolean indicating whether the scan is submitted or skipped
     * @param mandatoryIrisCallback Optional callback function for when the iris scan is mandatory, but no iris could be saved
     */
    fun confirmIrisScan(eye: IrisPosition, scanned: Boolean, mandatoryIrisCallback: (() -> Unit)? = null) {
        irisScans[eye] = scanned
        val isLastEye = eye == IrisPosition.values().last()
        //when finished with all the iris authentication steps we save the iris template. If no valid save state, screen will not advance.
        if (isLastEye && !saveIrisTemplate() && mandatoryIrisCallback != null) {
            mandatoryIrisCallback()
            return
        }
        navigateForward()
    }

    /**
     * Save the iris NETemplate file (.dat) to the cache dir
     * This does not generate a template, merely saves it,
     * so it assumes you have run the NBiometricsOperation.CREATE_TEMPLATE tasks before.
     *
     * @return  True if the template was saved, or if no irises scanned but not mandatory.
     *          False if iris scan was mandatory, but no irises were scanned.
     */
    @Throws(IOException::class)
    private fun saveIrisTemplate(): Boolean {

        // If no irises were scanned but it was mandatory, generate a dialog
        if (subject.irises.size == 0 && workflowItems.contains(WorkflowItem.IRIS_SCAN) && WorkflowItem.IRIS_SCAN.mandatory) {
            logInfo("Trying to save mandatory iris template, but no irises scanned")
            return false
        }

        if (subject.template != null && subject.template.irises != null) {
            logInfo("saveIrisTemplate: storing template with " + subject.irises.size + " iris(es)")
            val outputFile = tempTemplateFile
            NFile.writeAllBytes(outputFile.absolutePath, subject.template.save())
            subject.template.irises
            logInfo("saved template successfully: {}", outputFile)
        }
        return true
    }

    fun navigateBack(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val previousScreenIndex = screens.indexOf(currentScreen) - 1

        if (previousScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.BACKWARD
            this.currentScreen.set(screens[previousScreenIndex])
            return true
        }

        return false
    }

    private fun navigateForward(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val nextScreenIndex = screens.indexOf(currentScreen) + 1

        if (nextScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.FORWARD
            this.currentScreen.set(screens[nextScreenIndex])
            return true
        }

        return false
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_SCREENS, ArrayList(screens))
        outState.putParcelable(STATE_CURRENT_SCREEN, currentScreen.get())
        outState.putString(STATE_PARTICIPANT_ID, participantId.get())
        outState.putString(STATE_PHONE_COUNTRY_CODE, phoneCountryCode.get())
        outState.putString(STATE_PHONE_NUMBER, participantPhone.get())
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        screens = savedInstanceState.getParcelableArrayList<Screen>(STATE_SCREENS).orEmpty()
        currentScreen.set(savedInstanceState.getParcelable(STATE_CURRENT_SCREEN))
        participantId.set(savedInstanceState.getString(STATE_PARTICIPANT_ID))
        phoneCountryCode.set(savedInstanceState.getString(STATE_PHONE_COUNTRY_CODE))
        participantPhone.set(savedInstanceState.getString(STATE_PHONE_NUMBER))
    }

    fun reset() {
        navigationDirection = NavigationDirection.BACKWARD
        currentScreen.set(Screen.INTRO)
        participantId.set(null)
        isManualSetParticipantId.set(null)
        barcodeParticipantId.set(null)
        participantPhone.set(null)
        phoneCountryCode.set(null)
        subject.clear()
        irisIndexes.clear()
        irisScans.clear()
        tempTemplateFile.delete()
    }

    enum class WorkflowItem(val type: String, var mandatory: Boolean) {
        ID_CARD("id_card", false),
        PHONE("phone", false),
        IRIS_SCAN("iris_scan", false),
        MATCHING("", false),
        VISIT("", false);
    }

    enum class Screen(@StringRes val title: Int) : Parcelable {
        INTRO(R.string.match_or_register_patient_intro_title),
        PARTICIPANT_ID(R.string.participant_flow_participant_id_title),
        PHONE(R.string.participant_flow_phone_title),
        IRIS_SCAN_LEFT_EYE(R.string.iris_scan_left_title),
        IRIS_SCAN_RIGHT_EYE(R.string.iris_scan_right_title),
        PARTICIPANT_MATCHING(R.string.participant_matching_title);

        constructor(parcel: Parcel) : this(parcel.readInt())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(ordinal)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Screen> {
            override fun createFromParcel(parcel: Parcel): Screen {
                return values()[parcel.readInt()]
            }

            override fun newArray(size: Int): Array<Screen?> {
                return arrayOfNulls(size)
            }
        }
    }

}
