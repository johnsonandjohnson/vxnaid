package com.jnj.vaccinetracker.visit

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Manufacturer
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.dateDayStart
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for visit screen.
 *
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
class VisitViewModel @Inject constructor(
    private val participantManager: ParticipantManager,
    private val visitManager: VisitManager,
    private val configurationManager: ConfigurationManager,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
    private val sessionExpiryObserver: SessionExpiryObserver,
) : ViewModelBase() {

    /**
     * emits when event submission finished
     */
    val visitEvents = eventFlow<Boolean>()
    val otherVisitEvents = eventFlow<Boolean>()
    private val retryClickEvents = eventFlow<Unit>()
    private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
    val loading = mutableLiveBoolean()
    val participant = mutableLiveData<ParticipantSummaryUiModel>()
    val participantImage = mutableLiveData<ParticipantImageUiModel>()
    val dosingVisit = mutableLiveData<VisitDetail>()
    val dosingVisitIsInsideTimeWindow = mutableLiveBoolean()
    val previousDosingVisits = mutableLiveData<List<VisitDetail>>()
    val errorMessage = mutableLiveData<String>()
    val vialValidationMessage = mutableLiveData<String>()
    val selectedManufacturer = mutableLiveData<String>()
    val manufacturerList = mutableLiveData<List<String>>()
    val manufacturerValidationMessage = mutableLiveData<String>()
    val differentManufacturerAllowed = mutableLiveBoolean()
    private val manufacturerRegexes = mutableLiveData<List<Manufacturer>>()
    val upcomingVisit = mutableLiveData<UpcomingVisit?>()

    init {
        initState()
    }

    private suspend fun loadImage(participantSummary: ParticipantSummaryUiModel) {
        // If the picture is already loaded, don't need to load again
        if (participant.value == participantSummary && participantImage.get() != null) return
        // If we already have the picture (from match or registration), don't need to query it again
        if (participantSummary.participantPicture != null) {
            participantImage.set(participantSummary.participantPicture)
            return
        }

        try {
            val bytes = participantManager.getPersonImage(participantSummary.participantUuid)
            participantImage.value = bytes.toUiModel()
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logWarn("Failed to get person image: ", ex)
        }
    }

    private suspend fun load(participantSummary: ParticipantSummaryUiModel) {
        try {
            val visits = visitManager.getVisitsForParticipant(participantSummary.participantUuid)
            val manufacturers = configurationManager.getVaccineManufacturers(participantSummary.vaccine.value)
            val config = configurationManager.getConfiguration()
            onVisitsLoaded(visits)
            onManufacturersLoaded(manufacturers)
            differentManufacturerAllowed.set(config.canUseDifferentManufacturers)
            manufacturerRegexes.set(config.manufacturers)
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            logError("Failed to load visits for participant: ", ex)
        }

        loadImage(participantSummary)
    }

    private fun initState() {
        loading.set(true)
        errorMessage.set(null)
        participantArg.filterNotNull().onEach { participant ->
            this.participant.value = participant
        }.combine(retryClickEvents.asFlow()) { participant, _ ->
            loading.set(true)
            errorMessage.set(null)
            load(participant)
        }.launchIn(scope)

        retryClickEvents.tryEmit(Unit)

    }

    fun setArguments(participant: ParticipantSummaryUiModel) {
        this.participantArg.value = participant
    }

    fun onRetryClick() {
        retryClickEvents.tryEmit(Unit)
    }

    /**
     * Triggered when visits are loaded from manager.
     * Will set the previous visits for the dosing history and find the current open dosing visit.
     * Checks if the current time is in the dosing visit window for the current open dosing visit.
     *
     * @param visits    List of VisitDetail objects of the retrieved visits for this participant
     */
    private fun onVisitsLoaded(visits: List<VisitDetail>) {
        previousDosingVisits.set(visits.findPreviousDosingVisits())

        val foundDosingVisit = visits.findDosingVisit()
        dosingVisit.set(foundDosingVisit)

        foundDosingVisit?.let { visit ->
            val now = Calendar.getInstance().timeInMillis
            val insideTimeWindow = now in visit.startDate.dateDayStart.time..(visit.endDate.dateDayStart.time + 1.days)
            logInfo("insideTimeWindow: $insideTimeWindow")
            dosingVisitIsInsideTimeWindow.set(insideTimeWindow)
        }
    }

    /**
     * Triggered when manufacturers are loaded from the manager.
     * Will set the manufacturer dropdown list and default if there is only one option.
     *
     * @param manufacturers     List of strings representing the manufacturers possible for this regimen
     */
    private fun onManufacturersLoaded(manufacturers: List<String>) {
        this.manufacturerList.set(manufacturers)

        // If only one manufacturer possible, default to this value.
        if (manufacturers.size == 1) {
            this.selectedManufacturer.set(manufacturers[0])
        }
    }

    /**
     * Sets the selected manufacturer and removes validation message if present.
     *
     * @param manufacturerName  Manufacturer name to select
     */
    fun setSelectedManufacturer(manufacturerName: String) {
        if (this.selectedManufacturer.get() == manufacturerName) return
        selectedManufacturer.set(manufacturerName)
        manufacturerValidationMessage.set(null)
    }

    /**
     * Check if the barcode matches any of the regex strings for the manufacturers
     * As soon as a match is found, this manufacturer will be selected
     * If no matches are found, a validation message is shown on the manufacturer dropdown
     *
     * @param barcode           Scanned barcode string
     * @param resourcesWrapper  ResourcesWrapper to resolve resource string for validation message
     */
    fun matchBarcodeManufacturer(barcode: CharSequence, resourcesWrapper: ResourcesWrapper) {
        manufacturerRegexes.get()?.forEach {
            if (it.barcodeRegex.toRegex().containsMatchIn(barcode)) {
                setSelectedManufacturer(it.name)
                return@forEach
            }
        }
        if (selectedManufacturer.get().isNullOrEmpty()) {
            manufacturerValidationMessage.set(resourcesWrapper.getString(R.string.visit_dosing_error_no_manufacturer))
        }
    }

    /**
     * Check if the given manufacturer is the expected one for this dose number
     *
     * @param dosingNumber      Dosing number for the dosing visit
     * @param manufacturerName  Manufacturer selected for this dose
     * @return  True if this manufacturer was the one we expected for this dose, false if not.
     *          Also returns true if there is no dose number.
     */
    private fun isExpectedManufacturer(dosingNumber: Int?, manufacturerName: String): Boolean {
        if (dosingNumber == null) return true
        return manufacturerList.get()?.get(dosingNumber - 1) == manufacturerName
    }


    /**
     * Find the open dosing visit
     */
    private fun List<VisitDetail>.findDosingVisit(): VisitDetail? {
        return findLast { visit ->
            visit.visitType == Constants.VISIT_TYPE_DOSING && hasNotOccurredYet(visit)
        }
    }

    /**
     * Find previously completed dosing visits, ordered by dosing number
     */
    private fun List<VisitDetail>.findPreviousDosingVisits(): List<VisitDetail> {
        val previousDosingVisits = mutableListOf<VisitDetail>()

        map { visit ->
            if (visit.visitType == Constants.VISIT_TYPE_DOSING && visit.visitStatus == Constants.VISIT_STATUS_OCCURRED)
                previousDosingVisits.add(visit)
        }

        return previousDosingVisits.sortedBy { it.dosingNumber }

    }

    /**
     * Check if visit has not occurred yet
     */
    private fun hasNotOccurredYet(visit: VisitDetail): Boolean {
        return visit.visitStatus != Constants.VISIT_STATUS_MISSED && visit.visitStatus != Constants.VISIT_STATUS_OCCURRED
    }

    /**
     * Submit a dosing visit encounter
     *
     * @param vialBarcode                           String representing the vial barcode
     * @param outsideTimeWindowConfirmationListener Callback function for when the current time is outside the dosing window
     * @param incorrectManufacturerListener         Callback function for when an unexpected manufacturer was selected
     * @param overrideOutsideTimeWindowCheck        Indicate if the time window check should be skipped
     * @param overrideManufacturerCheck             Indicate if the manufacturer check should be skipped
     */
    @SuppressWarnings("LongParameterList")
    fun submitDosingVisit(
        vialBarcode: String,
        outsideTimeWindowConfirmationListener: () -> Unit,
        incorrectManufacturerListener: () -> Unit,
        overrideOutsideTimeWindowCheck: Boolean = false,
        overrideManufacturerCheck: Boolean = false,
    ) {

        val manufacturer = selectedManufacturer.get()
        val participant = participant.get()
        val dosingVisit = dosingVisit.get()

        vialValidationMessage.set(null)

        if (manufacturer.isNullOrEmpty()) {
            manufacturerValidationMessage.set(resourcesWrapper.getString(R.string.visit_dosing_error_no_manufacturer))
            return
        }

        if (participant == null || dosingVisit == null) {
            logError("No participant or dosing visit in memory!")
            visitEvents.tryEmit(false)
            return
        }

        if (!overrideOutsideTimeWindowCheck && !dosingVisitIsInsideTimeWindow.get()) {
            outsideTimeWindowConfirmationListener()
            return
        }

        if (!overrideManufacturerCheck && !isExpectedManufacturer(dosingVisit.dosingNumber, manufacturer)) {
            incorrectManufacturerListener()
            return
        }

        loading.set(true)

        scope.launch {
            try {
                visitManager.registerDosingVisit(
                    encounterDatetime = Date(),
                    visitUuid = dosingVisit.uuid,
                    vialCode = vialBarcode,
                    manufacturer = manufacturer,
                    participantUuid = participant.participantUuid,
                    dosingNumber = requireNotNull(dosingVisit.dosingNumber) { "dosing visit must have a dosing number" }
                )
                onVisitLogged()
                loading.set(false)
                visitEvents.tryEmit(true)
            } catch (ex: OperatorUuidNotAvailableException) {
                loading.set(false)
                sessionExpiryObserver.notifySessionExpired()
            } catch (throwable: Throwable) {
                yield()
                throwable.rethrowIfFatal()
                loading.set(false)
                logError("Failed to register dosing visit: ", throwable)
                visitEvents.tryEmit(false)
            }
        }
    }

    /**
     * Submit a visit of type 'other'
     *
     */
    fun submitOtherVisit() {
        val participant = participant.get()
        if (participant == null) {
            logError("No participant or dosing visit in memory!")
            visitEvents.tryEmit(false)
            return
        }

        loading.set(true)

        scope.launch {
            try {
                visitManager.registerOtherVisit(participant.participantUuid)
                onVisitLogged()
                loading.set(false)
                otherVisitEvents.tryEmit(true)
            } catch (ex: OperatorUuidNotAvailableException) {
                loading.set(false)
                sessionExpiryObserver.notifySessionExpired()
            } catch (throwable: Throwable) {
                yield()
                throwable.rethrowIfFatal()
                loading.set(false)
                logError("Failed to register other visit: ", throwable)
                otherVisitEvents.tryEmit(false)
            }
        }
    }

    private suspend fun onVisitLogged() {
        val participantUuid = participant.value?.participantUuid
        logInfo("onVisitLogged: participantUuid=$participantUuid")
        upcomingVisit.value = if (participantUuid != null) {
            try {
                visitManager.getUpcomingVisit(participantUuid)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("Failed to get upcoming visit", ex)
                null
            }
        } else {
            logWarn("error participantUuid not available to get upcoming visit")
            null
        }
    }
}

