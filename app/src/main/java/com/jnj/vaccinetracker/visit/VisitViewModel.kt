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
    val upcomingVisit = mutableLiveData<UpcomingVisit?>()

    private val weight = MutableLiveData<Int?>()
    val weightValidationMessage = mutableLiveData<String>()
    val zScoreWeightText = MutableLiveData<String>()

    private val height = MutableLiveData<Int?>()
    val heightValidationMessage = mutableLiveData<String>()
    val zScoreHeightText = MutableLiveData<String>()

    val zScoreNutritionText = MutableLiveData<String>()
    val zScoreNutritionTextColor = MutableLiveData<Int>()
    val zScoreNutritionPlaceholder = "-/-"
    val isOedema = MutableLiveData(false)
    val displayOedema = MutableLiveData(false)

    private val muac = MutableLiveData<Int?>()
    val muacValidationMessage = mutableLiveData<String>()
    val zScoreMuacText = MutableLiveData<String>()
    val shouldValidateMuac = MutableLiveData<Boolean>()
    val zScoreMuacTextColor = MutableLiveData<Int>()

    var substancesData = MutableLiveData(listOf<SubstanceDataModel>())
    var selectedSubstancesWithBarcodes = MutableLiveData<MutableMap<String, Map<String, String>>>(mutableMapOf())
    var selectedOtherSubstances = MutableLiveData<MutableMap<String, String>>()
    var otherSubstancesData =  MutableLiveData<List<OtherSubstanceDataModel>>(listOf())

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
            substancesData.value = SubstancesDataUtil.getSubstancesDataForCurrentVisit(
                participantSummary.birthDateText,
                visits,
                configurationManager
            )
            otherSubstancesData.value = SubstancesDataUtil.getOtherSubstancesDataForCurrentVisit(
                participantSummary.birthDateText,
                configurationManager
            )

            shouldValidateMuac.value =
                MuacZScoreCalculator.shouldCalculateMuacZScore(participantSummary.birthDateText)
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
            val insideTimeWindow =
                now in visit.startDate.dateDayStart.time..(visit.endDate.dateDayStart.time + 1.days)
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
        var isZScoreValid = true
        val weight = weight.value
        val height = height.value
        val muac = muac.value
        val shouldValidateMuac = shouldValidateMuac.value
        val isOedema = if (!displayOedema.value!!) false else isOedema.value
        val participant = participant.get()
        val dosingVisit = dosingVisit.get()
        val substancesObservations = selectedSubstancesWithBarcodes.value ?: mapOf()
        val otherSubstancesObservations = selectedOtherSubstances.value ?: mapOf()
        val missingSubstances = getMissingSubstanceLabels()

        if (participant == null || dosingVisit == null) {
            logError("No participant or dosing visit in memory!")
            visitEvents.tryEmit(false)
            return
        }

        if (weight == null) {
            weightValidationMessage.set(resourcesWrapper.getString(R.string.visit_dosing_error_no_weight))
            isZScoreValid = false
        }

        if (height == null) {
            heightValidationMessage.set(resourcesWrapper.getString(R.string.visit_dosing_error_no_height))
            isZScoreValid = false
        }

        if (shouldValidateMuac == true && muac == null) {
            muacValidationMessage.set(resourcesWrapper.getString(R.string.visit_dosing_error_no_muac))
            isZScoreValid = false
        }

        if (!isZScoreValid) return

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
                    dosingNumber = dosingVisit.dosingNumber ?: 0,
                    weight = weight!!,
                    height = height!!,
                    isOedema = isOedema!!,
                    muac = muac,
                    substanceObservations = substancesObservations.toMap(),
                    otherSubstanceObservations = otherSubstancesObservations.toMap(),
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

    fun setWeight(value: Int?) {
        val validatedValue = if (value != null && value < 0) 0 else value

        if (validatedValue == weight.value) return

        weight.value = validatedValue
        val zScore = participant.value?.let {
            WeightZScoreCalculator(
                validatedValue,
                it.gender,
                it.birthDateText
            ).calculateZScoreAndRating()
        }

        zScoreWeightText.value = zScore?.toString() ?: ""
        weightValidationMessage.set(null)
        setNutritionZScore()
    }

    fun setHeight(value: Int?) {
        val validatedValue = if (value != null && value < 0) 0 else value

        if (validatedValue == height.value) return

        height.value = validatedValue
        val zScore = participant.value?.let {
            HeightZScoreCalculator(
                validatedValue,
                it.gender,
                it.birthDateText
            ).calculateZScoreAndRating()
        }

        zScoreHeightText.value = zScore?.toString() ?: ""
        heightValidationMessage.set(null)
        setNutritionZScore()
    }

    fun setIsOedema(value: Boolean) {
        if (value == isOedema.value) return
        isOedema.value = value
        setNutritionZScore()
    }

    fun setMuac(value: Int?) {
        val validatedValue = if (value != null && value < 0) 0 else value

        if (validatedValue == height.value) return

        muac.value = validatedValue
        val muacZScoreCalculator = participant.value?.let {
            MuacZScoreCalculator(
                validatedValue,
                it.gender,
                it.birthDateText
            )
        } ?: return

        val zScore = muacZScoreCalculator.calculateZScoreAndRating()
        zScoreMuacText.value = zScore?.toString() ?: ""
        zScoreMuacTextColor.value = muacZScoreCalculator.getTextColorBasedOnZsCoreValue()
        muacValidationMessage.set(null)
    }

    private fun setNutritionZScore() {
        val nutritionZScoreCalculator = participant.value?.let {
            NutritionZScoreCalculator(
                weight.value,
                height.value,
                isOedema.value,
                it.gender,
                it.birthDateText
            )
        } ?: return

        val zScore = nutritionZScoreCalculator.calculateZScoreAndRating()
        displayOedema.value = nutritionZScoreCalculator.isOedemaValue()
        zScoreNutritionText.value = zScore?.toString()
        zScoreNutritionTextColor.value = nutritionZScoreCalculator.getTextColorBasedOnZsCoreValue()
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

        return substancesData.value
            ?.filter { it.conceptName !in selectedConceptNames }
           ?.map { it.label } ?: listOf()
    }
}

