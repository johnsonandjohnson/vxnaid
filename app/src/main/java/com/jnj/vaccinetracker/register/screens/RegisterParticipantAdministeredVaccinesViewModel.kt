package com.jnj.vaccinetracker.register.screens

import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Manufacturer
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Date
import javax.inject.Inject

class RegisterParticipantAdministeredVaccinesViewModel @Inject constructor(
   override val dispatchers: AppCoroutineDispatchers,
   private val configurationManager: ConfigurationManager,
   private val resourcesWrapper: ResourcesWrapper,
   private val visitManager: VisitManager,
   private val sessionExpiryObserver: SessionExpiryObserver,
) : ViewModelBase() {

   val registerVaccinesSuccessEvents = eventFlow<ParticipantSummaryUiModel>()
   var substancesData = mutableLiveData<List<SubstanceDataModel>>()
   var selectedSubstances = mutableLiveData<MutableList<SubstanceDoseDataModel>>()
   val loading = mutableLiveBoolean()
   val errorMessage = mutableLiveData<String>()
   private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
   val participant = mutableLiveData<ParticipantSummaryUiModel>()
   private val retryClickEvents = eventFlow<Unit>()
   val dosingVisit = mutableLiveData<VisitDetail>()

   data class SubstanceDoseDataModel(
      val substance: SubstanceDataModel,
      val dose: Int
   )

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

   fun addSelectedSubstance(substance: SubstanceDataModel, dose: Int) {
      val newSubstance = SubstanceDoseDataModel(substance, dose)
      selectedSubstances.value?.add(newSubstance)
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

   fun submitVaccineRegistration(
   ) {
      scope.launch {
         doRegisterVisit()
      }
   }

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


      loading.set(true)

      scope.launch {
         try {
            // TODO change to multiple substances
            visitManager.registerDosingVisit(
               encounterDatetime = Date(),
               visitUuid = dosingVisit.uuid,
               vialCode = "",
               manufacturer = "",
               participantUuid = participant.participantUuid,
               dosingNumber = requireNotNull(dosingVisit.dosingNumber) { "dosing visit must have a dosing number" },
               weight = null,
               height = null,
               isOedema = null,
               muac = null
            )
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
