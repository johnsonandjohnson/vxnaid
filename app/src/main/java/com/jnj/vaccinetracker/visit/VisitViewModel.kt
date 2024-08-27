package com.jnj.vaccinetracker.visit

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.dateDayStart
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.jnj.vaccinetracker.visit.zscore.HeightZScoreCalculator
import com.jnj.vaccinetracker.visit.zscore.MuacZScoreCalculator
import com.jnj.vaccinetracker.visit.zscore.NutritionZScoreCalculator
import com.jnj.vaccinetracker.visit.zscore.WeightZScoreCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for visit screen.
 *
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitViewModel @Inject constructor(
    private val participantManager: ParticipantManager,
    private val visitManager: VisitManager,
    private val configurationManager: ConfigurationManager,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val createVisitUseCase: CreateVisitUseCase,
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    ) : ViewModelBase() {

    /**
     * emits when event submission finished
     */
    val visitEvents = eventFlow<Boolean>()
    private val retryClickEvents = eventFlow<Unit>()
    private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
    val loading = mutableLiveBoolean()
    val participant = mutableLiveData<ParticipantSummaryUiModel>()
    val participantImage = mutableLiveData<ParticipantImageUiModel>()
    val dosingVisit = mutableLiveData<VisitDetail>()
    val dosingVisitIsInsideTimeWindow = mutableLiveBoolean()
    val previousDosingVisits = mutableLiveData<List<VisitDetail>>()
    val errorMessage = mutableLiveData<String>()
    val upcomingVisit = mutableLiveData<UpcomingVisit?>()

    var suggestedSubstancesData = MutableLiveData(listOf<SubstanceDataModel>())
    var selectedSubstancesData = MutableLiveData(listOf<SubstanceDataModel>())
    var substancesDataAll = MutableLiveData(listOf<SubstanceDataModel>())
    var selectedSubstancesWithBarcodes = MutableLiveData<MutableMap<String, Map<String, String>>>(mutableMapOf())
    var selectedOtherSubstances = MutableLiveData<MutableMap<String, String>>()
    var otherSubstancesData =  MutableLiveData<List<OtherSubstanceDataModel>>(listOf())
    var checkOtherSubstances =  MutableLiveData<Boolean>(false)
    var isAnyOtherSubstancesEmpty =  MutableLiveData<Boolean>(false)
    var visitsCounter = MutableLiveData<Int>(0)

    var isSuggesting =  MutableLiveData<Boolean>(true)
    var selectedVisitType =  MutableLiveData<String>(Constants.VISIT_TYPES[0])
    var suggestedVisitType =  MutableLiveData<String>(Constants.VISIT_TYPES[0])
    var visitTypes =  MutableLiveData<List<String>>(Constants.VISIT_TYPES)
    var visitLocation = MutableLiveData<String>()
    var isVisitLocationSelected = MutableLiveData(false)
    var checkVisitLocation = MutableLiveData(false)

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

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun load(participantSummary: ParticipantSummaryUiModel) {
        try {
            val visits = visitManager.getVisitsForParticipant(participantSummary.participantUuid)
            visitsCounter.value = visits.count()
            suggestedVisitType.value = SubstancesDataUtil.getVisitTypeForCurrentVisit(participantSummary.birthDateText)
            selectedVisitType.value = suggestedVisitType.value
            suggestedSubstancesData.value = SubstancesDataUtil.getSubstancesDataForCurrentVisit(
                participantSummary.birthDateText,
                visits,
                configurationManager
            )
            selectedSubstancesData.value = suggestedSubstancesData.value
            substancesDataAll.value = SubstancesDataUtil.getAllSubstances(configurationManager)
            otherSubstancesData.value = SubstancesDataUtil.getOtherSubstancesDataForCurrentVisit(
                participantSummary.birthDateText,
                configurationManager
            )
            onVisitsLoaded(visits)
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

    @RequiresApi(Build.VERSION_CODES.O)
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
            val startTime = visit.startDate.dateDayStart.time
            val endTime = visit.endDate.dateDayStart.time + 1.days
            val insideTimeWindow = now >= startTime && now <= endTime
            logInfo("insideTimeWindow: $insideTimeWindow")
            dosingVisitIsInsideTimeWindow.set(insideTimeWindow)
        }
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
     * @param outsideTimeWindowConfirmationListener Callback function for when the current time is outside the dosing window
     * @param missingSubstancesListener             Callback function for when some of the selected vaccines were not administered
     * @param overrideOutsideTimeWindowCheck        Indicate if the time window check should be skipped
     * @param newVisitDate                          Specify next visit date
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressWarnings("LongParameterList")
    fun submitDosingVisit(
        outsideTimeWindowConfirmationListener: () -> Unit,
        missingSubstancesListener: (List<String>) -> Unit,
        overrideOutsideTimeWindowCheck: Boolean = false,
        newVisitDate: Date? = null
    ) {
        val participant = participant.get()
        val dosingVisit = dosingVisit.get()
        val visitsCounter = visitsCounter.value
        val substancesObservations = selectedSubstancesWithBarcodes.value ?: mapOf()
        val otherSubstancesObservations = selectedOtherSubstances.value ?: mapOf()
        val missingSubstances = getMissingSubstanceLabels()
        val visitLocationValue = visitLocation.value

        if (participant == null || dosingVisit == null) {
            logError("No participant or dosing visit in memory!")
            visitEvents.tryEmit(false)
            return
        }

        if (!overrideOutsideTimeWindowCheck && !dosingVisitIsInsideTimeWindow.get() && newVisitDate == null) {
            outsideTimeWindowConfirmationListener()
            return
        }

        if (newVisitDate == null && missingSubstances.isNotEmpty()
        ) {
            missingSubstancesListener(missingSubstances)
            return
        }

        loading.set(true)

        scope.launch {
            try {
                visitManager.registerDosingVisit(
                    encounterDatetime = Date(),
                    visitUuid = dosingVisit.uuid,
                    participantUuid = participant.participantUuid,
                    dosingNumber = visitsCounter ?: 0,
                    substanceObservations = substancesObservations.toMap(),
                    otherSubstanceObservations = otherSubstancesObservations.toMap(),
                    visitLocation = visitLocationValue
                )

                // schedule next visit after submitting current one
                createNextVisit(participant, newVisitDate)

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

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createNextVisit(participant: ParticipantSummaryUiModel, newVisitDate: Date? = null) {
        val visitDate = newVisitDate ?: getNextVisitDate(participant)
        createVisitUseCase.createVisit(buildVisitObject(participant, visitDate!!))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getNextVisitDate(participant: ParticipantSummaryUiModel): Date? {
        val weeksNumberAfterBirthForNextVisit = findWeeksNumberAfterBirthForNextVisit(participant.birthDateText)
        return weeksNumberAfterBirthForNextVisit?.let { calculateNextVisitDate(participant.birthDateText, it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun findWeeksNumberAfterBirthForNextVisit(participantBirthDate: String): Int? {
        val substancesConfig = configurationManager.getSubstancesConfig()
        val weeksAfterBirthSet = substancesConfig.map { it.weeksAfterBirth }.sorted().toSet()
        val childAgeInWeeks = SubstancesDataUtil.getWeeksBetweenDateAndToday(participantBirthDate)

        return substancesConfig
            .filter {
                val minWeekNumber = it.weeksAfterBirth - it.weeksAfterBirthLowWindow
                val maxWeekNumber = it.weeksAfterBirth + it.weeksAfterBirthUpWindow
                childAgeInWeeks in minWeekNumber..maxWeekNumber
            }.firstNotNullOfOrNull {
                weeksAfterBirthSet.filter { week -> week > it.weeksAfterBirth }.minOrNull()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildVisitObject(participant: ParticipantSummaryUiModel, visitDate: Date): CreateVisit {
        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Operator uuid not available")
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Location not available")

        return CreateVisit(
            participantUuid = participant.participantUuid,
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = visitDate,
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateNextVisitDate(birthDateText: String, weeksNumberAfterBirth: Int): Date {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val birthDate = LocalDate.parse(birthDateText, formatter)
        val nextVisitDate = birthDate.plusWeeks(weeksNumberAfterBirth.toLong())
        return Date.from(nextVisitDate.atStartOfDay(ZoneId.of(Constants.UTC_TIME_ZONE_NAME)).toInstant())
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

    fun addObsToObsMap(conceptName: String, barcode: String, manufacturerName: String) {
        val currentMap = selectedSubstancesWithBarcodes.value?.toMutableMap() ?: mutableMapOf()
        currentMap[conceptName] = mapOf(Constants.BARCODE_STR to barcode, Constants.MANUFACTURER_NAME_STR to manufacturerName)
        selectedSubstancesWithBarcodes.postValue(currentMap)
    }

    fun addObsToOtherSubstancesObsMap(conceptName: String, value: String) {
        val currentMap = selectedOtherSubstances.value?.toMutableMap() ?: mutableMapOf()
        currentMap[conceptName] = value
        selectedOtherSubstances.postValue(currentMap)
    }

    fun removeObsFromMap(conceptName: String) {
        val currentMap = selectedSubstancesWithBarcodes.value?.toMutableMap() ?: mutableMapOf()
        currentMap.remove(conceptName)
        selectedSubstancesWithBarcodes.postValue(currentMap)
    }

    private fun getMissingSubstanceLabels(): List<String> {
        val selectedConceptNames = selectedSubstancesWithBarcodes.value?.keys?.toSet() ?: setOf()

        return suggestedSubstancesData.value
            ?.filter { it.conceptName !in selectedConceptNames }
           ?.map { it.label } ?: listOf()
    }

    fun checkIfAnyOtherSubstancesEmpty() {
        checkOtherSubstances.value = true
    }

    fun setIsSuggesting(checked: Boolean) {
        if (checked == isSuggesting.value) return
        isSuggesting.value = checked
        selectedSubstancesData.value = suggestedSubstancesData.value
        selectedVisitType.value = suggestedVisitType.value
    }

    fun addToSelectedSubstances(vaccine: SubstanceDataModel) {
        selectedSubstancesData.value = selectedSubstancesData.value?.plus(vaccine)
    }

    fun removeFromSelectedSubstances(vaccine: SubstanceDataModel) {
        selectedSubstancesData.value = selectedSubstancesData.value?.minus(vaccine)
    }

    suspend fun onVisitTypeDropdownChange() {
        selectedSubstancesData.value = SubstancesDataUtil.getSubstancesDataForVisitType(
            selectedVisitType.value ?: "",
            configurationManager
        )
        otherSubstancesData.value = SubstancesDataUtil.getOtherSubstancesDataForVisitType(
            selectedVisitType.value ?: "",
            configurationManager
        )
        removeSelectedOtherSubstancesIfNotRelatedToVisitType()
    }

    private fun removeSelectedOtherSubstancesIfNotRelatedToVisitType() {
        val conceptNames = otherSubstancesData.value?.map { it.conceptName } ?: emptyList()
        val currentData = selectedOtherSubstances.value?.toMutableMap()
        currentData?.entries?.removeIf { (key, _) ->
            key !in conceptNames
        }
        selectedOtherSubstances.value = currentData ?: mutableMapOf()
    }

    fun setVisitLocationValue(locationValue: String) {
        visitLocation.value = locationValue
    }

    fun isVisitLocationValid() : Boolean {
        return isVisitLocationSelected.value == true
    }

    fun checkVisitLocationSelection() {
        checkVisitLocation.value = true
    }
}

