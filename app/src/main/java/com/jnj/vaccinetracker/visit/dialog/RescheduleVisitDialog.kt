package com.jnj.vaccinetracker.visit.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRescheduleVisitBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.dialogs.ScheduleVisitDatePickerDialog
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.screens.ContraindicationsViewModel
import com.jnj.vaccinetracker.visit.screens.ReferralActivity
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

class RescheduleVisitDialog @Inject constructor() : BaseDialogFragment(), ScheduleVisitDatePickerDialog.OnDateSelectedListener {
   private lateinit var binding: DialogRescheduleVisitBinding
   private val viewModel: ContraindicationsViewModel by activityViewModels { viewModelFactory }
   private lateinit var rescheduleReasonEditText: EditText
   private lateinit var visitDateTextView: TextView
   private var visitDate: DateTime? = null
   private val participant: ParticipantSummaryUiModel? by lazy { requireArguments().getParcelable(PARTICIPANT) }
   @Inject lateinit var createVisitUseCase: CreateVisitUseCase
   @Inject lateinit var userRepository: UserRepository
   @Inject lateinit var syncSettingsRepository: SyncSettingsRepository
   @Inject lateinit var configurationManager: ConfigurationManager
   @Inject lateinit var visitManager: VisitManager
   @Inject lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource
   private lateinit var currentVisitUuid: String

   companion object {
      private const val PARTICIPANT = "participant"
      private const val CURRENT_VISIT_UUID = "currentVisitUuid"
      private const val PARTICIPANT_UUID = "participantUuid"

      fun create(participant: ParticipantSummaryUiModel?): RescheduleVisitDialog {
         return RescheduleVisitDialog().apply { arguments = bundleOf(PARTICIPANT to participant) }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
      lifecycleScope.launch {
         val allVisits = visitManager.getVisitsForParticipant(participant!!.participantUuid)
         currentVisitUuid = allVisits.findDosingVisit()!!.uuid
      }
   }

   @RequiresApi(Build.VERSION_CODES.O)
   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_reschedule_visit, container, false)
      binding.executePendingBindings()

      visitDateTextView = binding.nextVisitDateValue
      rescheduleReasonEditText = binding.editTextRescheduleReason

      binding.nextVisitDatePickerButton.setOnClickListener {
         ScheduleVisitDatePickerDialog(visitDate, this).show(childFragmentManager, "scheduleVisitDatePickerDialog")
      }

      binding.btnSaveVisit.setOnClickListener {
         lifecycleScope.launch {
            try {
               validateDate()
               if (visitDate != null) {
                  createVisitUseCase.createVisit(buildNextVisitObject(participant, Date(visitDate!!.unixMillisLong)))

                  if (rescheduleReasonEditText.text.toString().isNotEmpty()) {
                     val attributesToAdd = mutableMapOf(Constants.RESCHEDULE_VISIT_REASON_ATTRIBUTE_TYPE_NAME to rescheduleReasonEditText.text.toString())
                     vaccineTrackerSyncApiDataSource.updateVisitAttributes(currentVisitUuid, attributesToAdd)
                  }

                  dismissAllowingStateLoss()

                  val intent = Intent(requireContext(), ReferralActivity::class.java).apply {
                     putExtra(CURRENT_VISIT_UUID, currentVisitUuid)
                     putExtra(PARTICIPANT_UUID, participant?.participantUuid)
                  }
                  startActivity(intent)
               }
            } catch (ex: Exception) {
               viewModel.errorMessage.set(getString(R.string.reschedule_visit_failed))
            }
         }
      }

      binding.btnFinish.setOnClickListener {
         dismissAllowingStateLoss()

         val intent = Intent(requireContext(), ReferralActivity::class.java).apply {
            putExtra(CURRENT_VISIT_UUID, currentVisitUuid)
            putExtra(PARTICIPANT_UUID, participant?.participantUuid)
         }
         startActivity(intent)
      }
      return binding.root
   }

   override fun onDismiss(dialog: DialogInterface) {
      super.onDismiss(dialog)
      findParent<VisitRegisteredSuccessDialogListener>()?.onVisitRegisteredSuccessDialogClosed()
   }

   override fun onDateSelected(dateTime: DateTime) {
      visitDate = dateTime
      visitDateTextView.text = dateTime.format(DateFormat.FORMAT_DATE)
      binding.nextVisitDateValue.error = null
   }

   @RequiresApi(Build.VERSION_CODES.O)
   fun buildNextVisitObject(
      participant: ParticipantSummaryUiModel?,
      visitDate: Date
   ): CreateVisit {
      val operatorUuid = userRepository.getUser()?.uuid
         ?: throw OperatorUuidNotAvailableException("Operator uuid not available")
      val locationUuid = syncSettingsRepository.getSiteUuid()
         ?: throw NoSiteUuidAvailableException("Location not available")
      return CreateVisit(
         participantUuid = participant!!.participantUuid,
         visitType = Constants.VISIT_TYPE_DOSING,
         startDatetime = visitDate,
         locationUuid = locationUuid,
         attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
         )
      )
   }

   private fun validateDate() {
      if (visitDate == null) {
         val dateValidationText = getString(R.string.dialog_missing_substances_empty_date_validation_message)
         binding.nextVisitDateValue.error = dateValidationText
         val hintTextColor = ContextCompat.getColor(requireContext(), R.color.errorLight)
         binding.nextVisitDateValue.setHintTextColor(hintTextColor)
      }
   }

   interface VisitRegisteredSuccessDialogListener {
      fun onVisitRegisteredSuccessDialogClosed()
   }

   private fun List<VisitDetail>.findDosingVisit(): VisitDetail? {
      return findLast { visit ->
         visit.visitType == Constants.VISIT_TYPE_DOSING && hasNotOccurredYet(visit)
      }
   }

   private fun hasNotOccurredYet(visit: VisitDetail): Boolean {
      return visit.visitStatus != Constants.VISIT_STATUS_MISSED && visit.visitStatus != Constants.VISIT_STATUS_OCCURRED
   }
}