package com.jnj.vaccinetracker.register.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class RegisterParticipantAdministeredVaccinesViewModel @Inject constructor(
   override val dispatchers: AppCoroutineDispatchers,
   private val configurationManager: ConfigurationManager,
   private val resourcesWrapper: ResourcesWrapper,
   private val visitManager: VisitManager,
   private val sessionExpiryObserver: SessionExpiryObserver,
   private val createVisitUseCase: CreateVisitUseCase,
   private val userRepository: UserRepository,
   private val syncSettingsRepository: SyncSettingsRepository,
) : ViewModelBase() {

   val registerVaccinesSuccessEvents = eventFlow<ParticipantSummaryUiModel>()
   var substancesData = mutableLiveData<List<SubstanceDataModel>>(emptyList())
   var selectedSubstances = mutableLiveData<List<SubstanceDataModel>>()
   val selectedSubstancesDates = MutableLiveData<MutableMap<String, Map<String, String>>>(mutableMapOf())
   val loading = mutableLiveBoolean()
   val errorMessage = mutableLiveData<String>()
   private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
   val participant = mutableLiveData<ParticipantSummaryUiModel>()
   private val retryClickEvents = eventFlow<Unit>()
   private val dosingVisit = mutableLiveData<VisitDetail>()

   init {
      initState()
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

   private fun onVisitsLoaded(visits: List<VisitDetail>) {
      val foundDosingVisit = visits[0]
      dosingVisit.set(foundDosingVisit)
   }

   fun addSelectedSubstance(substance: SubstanceDataModel) {
      val currentSubstances = selectedSubstances.value ?: emptyList()
      selectedSubstances.value = currentSubstances + substance
   }

   fun removeFromSelectedSubstances(substanceToRemove: SubstanceDataModel) {
      val currentSubstances = selectedSubstances.value ?: emptyList()
      val updatedList = currentSubstances.filter { it.conceptName != substanceToRemove.conceptName }
      selectedSubstances.value = updatedList

      val currentDatesMap = selectedSubstancesDates.value ?: mutableMapOf()
      currentDatesMap.remove(substanceToRemove.conceptName)
      selectedSubstancesDates.value = currentDatesMap
   }

   fun addVaccineDate(conceptName: String, dateValue: String) {
      val currentMap = selectedSubstancesDates.value ?: mutableMapOf()
      currentMap[conceptName] = mapOf(Constants.DATE_STR to dateValue)
      selectedSubstancesDates.postValue(currentMap)
   }

   private suspend fun load(participantSummary: ParticipantSummaryUiModel) {
      withContext(dispatchers.io) {
         try {
            val visits = visitManager.getVisitsForParticipant(participantSummary.participantUuid)
            substancesData.set(SubstancesDataUtil.getAllSubstances(configurationManager))
            onVisitsLoaded(visits)
            loading.set(false)
         } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
         }
      }
   }

   fun setArguments(participant: ParticipantSummaryUiModel?) {
      this.participantArg.value = participant
   }

   @RequiresApi(Build.VERSION_CODES.O)
   fun submitVaccineRegistration(
   ) {
      scope.launch {
         doRegisterVisit()
      }
   }

   @RequiresApi(Build.VERSION_CODES.O)
   suspend fun createNextVisit(participant: ParticipantSummaryUiModel) {
         createVisitUseCase.createVisit(
            buildNextVisitObject(
               participant,
            )
         )
   }

   @RequiresApi(Build.VERSION_CODES.O)
   fun buildNextVisitObject(
      participant: ParticipantSummaryUiModel,
   ): CreateVisit {
      val operatorUuid = userRepository.getUser()?.uuid
         ?: throw OperatorUuidNotAvailableException("Operator uuid not available")
      val locationUuid = syncSettingsRepository.getSiteUuid()
         ?: throw NoSiteUuidAvailableException("Location not available")
      val nextVisitDate = LocalDate.now()
      return CreateVisit(
         participantUuid = participant.participantUuid,
         visitType = Constants.VISIT_TYPE_DOSING,
         startDatetime = convertLocalDateToDate(nextVisitDate),
         locationUuid = locationUuid,
         attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
         )
      )
   }
   @RequiresApi(Build.VERSION_CODES.O)
   fun convertLocalDateToDate(localDate: LocalDate): Date {
      val localDateTime = localDate.atStartOfDay()
      val instant = localDateTime.atZone(ZoneId.of(Constants.UTC_TIME_ZONE_NAME)).toInstant()
      return Date.from(instant)
   }

   @RequiresApi(Build.VERSION_CODES.O)
   @SuppressWarnings("LongParameterList")
   fun doRegisterVisit(
   ) {
      val participant = participant.get()
      val dosingVisit = dosingVisit.get()

      if (participant == null || dosingVisit == null) {
         logError("No participant or dosing visit in memory!")
         return
      }

      if (selectedSubstances.value?.isEmpty() == true || selectedSubstances.value == null) {
         registerVaccinesSuccessEvents.tryEmit(participant)
         return
      }

      val substanceObservations = selectedSubstances.value?.associate { substance ->
         substance.conceptName to mapOf(Constants.BARCODE_STR to "", Constants.MANUFACTURER_NAME_STR to "")
      }.orEmpty().toMutableMap()

      val substancesDatesMap = selectedSubstancesDates.value ?: mutableMapOf()
      substancesDatesMap.forEach { (conceptName, valueMap) ->
         val existingMap = substanceObservations[conceptName] ?: emptyMap()
         substanceObservations[conceptName] = existingMap + valueMap
      }

      loading.set(true)

      scope.launch {
         try {
            visitManager.registerDosingVisit(
               encounterDatetime = Date(),
               visitUuid = dosingVisit.uuid,
               participantUuid = participant.participantUuid,
               dosingNumber = dosingVisit.dosingNumber ?: 0,
               substanceObservations = substanceObservations,
               otherSubstanceObservations = null
            )
            createNextVisit(participant)
            loading.set(false)
            registerVaccinesSuccessEvents.tryEmit(participant)
         } catch (ex: OperatorUuidNotAvailableException) {
            loading.set(false)
            sessionExpiryObserver.notifySessionExpired()
         } catch (throwable: Throwable) {
            yield()
            throwable.rethrowIfFatal()
            loading.set(false)
            logError("Failed to register dosing visit: ", throwable)
         }
      }
   }
}
