package com.jnj.vaccinetracker.register.screens

import android.os.Build
import androidx.annotation.RequiresApi
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class RegisterParticipantHistoricalDataViewModel @Inject constructor(
   override val dispatchers: AppCoroutineDispatchers,
   private val visitManager: VisitManager,
   private val sessionExpiryObserver: SessionExpiryObserver,
   private val createVisitUseCase: CreateVisitUseCase,
   private val userRepository: UserRepository,
   private val syncSettingsRepository: SyncSettingsRepository,
) : ViewModelBase() {

   val registerVaccinesSuccessEvents = eventFlow<ParticipantSummaryUiModel>()
   val loading = mutableLiveBoolean()
   val errorMessage = mutableLiveData<String>()
   private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
   val participant = mutableLiveData<ParticipantSummaryUiModel>()
   private val dosingVisit = mutableLiveData<VisitDetail>()

   val visitTypesData = mutableLiveData<MutableMap<String, MutableMap<String, MutableMap<String, String>>>>(mutableMapOf())

   init {
      participantArg
         .filterNotNull()
         .distinctUntilChanged()
         .onEach { participant.value = it }
         .launchIn(scope)
   }

   @RequiresApi(Build.VERSION_CODES.O)
   suspend fun createNextVisit(participant: ParticipantSummaryUiModel) {
      createVisitUseCase.createVisit(buildNextVisitObject(participant))
   }

   private fun buildNextVisitObject(participant: ParticipantSummaryUiModel): CreateVisit {
      val operatorUuid = userRepository.getUser()?.uuid
         ?: throw OperatorUuidNotAvailableException("Operator UUID not available")
      val locationUuid = syncSettingsRepository.getSiteUuid()
         ?: throw NoSiteUuidAvailableException("Location not available")
      return CreateVisit(
         participantUuid = participant.participantUuid,
         visitType = Constants.VISIT_TYPE_DOSING,
         startDatetime = convertLocalDateToDate(LocalDate.now()),
         locationUuid = locationUuid,
         attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid
         )
      )
   }

   private fun convertLocalDateToDate(localDate: LocalDate): Date {
      val instant = localDate.atStartOfDay(ZoneId.of(Constants.UTC_TIME_ZONE_NAME)).toInstant()
      return Date.from(instant)
   }

   private suspend fun doRegisterVisit(visitTypeData: MutableMap<String, MutableMap<String, String>>) {
      val participant = participantArg.value ?: run {
         logError("No participant available.")
         return
      }
      this.participant.value = participant

      val visits = visitManager.getVisitsForParticipant(participant.participantUuid)
      onVisitsLoaded(visits)

      val dosingVisit = dosingVisit.value ?: run {
         logError("No dosing visit available.")
         return
      }

      val substanceObservations = visitTypeData[Constants.SUBSTANCES_AND_DATES_STR]?.mapValues {
         mapOf(
            Constants.DATE_STR to it.value,
            Constants.MANUFACTURER_NAME_STR to "",
            Constants.BARCODE_STR to ""
         )
      }?.toMutableMap() ?: mutableMapOf()

      val otherSubstancesAndValues = visitTypeData[Constants.OTHER_SUBSTANCES_AND_VALUES_STR] ?: mutableMapOf()

      loading.set(true)

      scope.launch {
         try {
            visitManager.registerDosingVisit(
               encounterDatetime = Date(),
               visitUuid = dosingVisit.uuid,
               participantUuid = participant.participantUuid,
               dosingNumber = dosingVisit.dosingNumber ?: 0,
               substanceObservations = substanceObservations,
               otherSubstanceObservations = otherSubstancesAndValues
            )
            createNextVisit(participant)
         } catch (ex: OperatorUuidNotAvailableException) {
            sessionExpiryObserver.notifySessionExpired()
         } catch (throwable: Throwable) {
            throwable.rethrowIfFatal()
            logError("Failed to register dosing visit: ", throwable)
         } finally {
            loading.set(false)
         }
      }
   }

   private fun onVisitsLoaded(visits: List<VisitDetail>) {
      dosingVisit.value = visits.firstOrNull()
   }

   suspend fun submitVaccineRegistration() {
      val visitData = visitTypesData.value ?: run {
         logError("No visit types data available.")
         return
      }

      visitData.values.forEach { visitTypeData ->
         doRegisterVisit(visitTypeData)
      }

      participant.value?.let {
         registerVaccinesSuccessEvents.tryEmit(it)
      } ?: logError("No participant to emit success event.")
   }

   fun setArguments(participant: ParticipantSummaryUiModel?) {
      participantArg.value = participant
   }

   fun addVisitTypeData(
      visitTypeName: String,
      substancesAndDates: MutableMap<String, String>?,
      otherSubstancesAndValues: MutableMap<String, String>
   ) {
      val currentData = visitTypesData.value ?: mutableMapOf()
      val visitTypeEntry = currentData.getOrPut(visitTypeName) { mutableMapOf() }

      visitTypeEntry[Constants.SUBSTANCES_AND_DATES_STR] = substancesAndDates ?: mutableMapOf()
      visitTypeEntry[Constants.OTHER_SUBSTANCES_AND_VALUES_STR] = otherSubstancesAndValues

      visitTypesData.postValue(currentData)
   }
}
