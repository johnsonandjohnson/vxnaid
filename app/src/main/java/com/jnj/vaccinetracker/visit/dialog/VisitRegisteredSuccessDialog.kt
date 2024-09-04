package com.jnj.vaccinetracker.visit.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.databinding.DialogVistRegisteredSuccessBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.dialogs.ScheduleVisitDatePickerDialog
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import com.jnj.vaccinetracker.visit.screens.ReferralActivity
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

/**
 * @author timonelen
 * @version 1
 */
class VisitRegisteredSuccessDialog : BaseDialogFragment(), ScheduleVisitDatePickerDialog.OnDateSelectedListener {

    companion object {
        private const val ARG_NEXT_VISIT = "next_visit"
        private const val PARTICIPANT = "participant"
        private const val CURRENT_VISIT_UUID = "currentVisitUuid"
        private const val PARTICIPANT_UUID = "participantUuid"

        fun create(nextVisit: UpcomingVisit?, participant: ParticipantSummaryUiModel?, currentVisitUuid: String?): VisitRegisteredSuccessDialog {
            return VisitRegisteredSuccessDialog().apply { arguments = bundleOf(ARG_NEXT_VISIT to nextVisit, PARTICIPANT to participant, CURRENT_VISIT_UUID to currentVisitUuid) }
        }
    }

    private lateinit var binding: DialogVistRegisteredSuccessBinding
    private lateinit var visitDateTextView: TextView
    private lateinit var visitScheduleResultTextView: TextView
    private lateinit var nextVisitDateContainerLinearLayout: LinearLayout
    private lateinit var proposedDateContainerLinearLayout: LinearLayout
    private lateinit var saveVisitButton: Button
    private lateinit var closeButton: Button
    private lateinit var proposedDateTextVisit: TextView
    private val nextVisit: UpcomingVisit? by lazy { requireArguments().getParcelable(ARG_NEXT_VISIT) }
    private var visitDate: DateTime? = null
    private val participant: ParticipantSummaryUiModel? by lazy { requireArguments().getParcelable(PARTICIPANT) }
    private val currentVisitUuid: String? by lazy { requireArguments().getString(CURRENT_VISIT_UUID) }
    @Inject lateinit var createVisitUseCase: CreateVisitUseCase
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var syncSettingsRepository: SyncSettingsRepository
    @Inject lateinit var configurationManager: ConfigurationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_vist_registered_success, container, false)
        binding.nextVisit = nextVisit
        binding.executePendingBindings()
        visitDateTextView = binding.root.findViewById(R.id.next_visit_date_value)
        visitScheduleResultTextView = binding.root.findViewById(R.id.visit_schedule_result_label)
        nextVisitDateContainerLinearLayout = binding.root.findViewById(R.id.next_visit_date_container)
        proposedDateContainerLinearLayout = binding.root.findViewById(R.id.proposed_next_visit_date_container)
        saveVisitButton = binding.root.findViewById(R.id.btn_save_visit)
        closeButton = binding.root.findViewById(R.id.btn_finish)
        proposedDateTextVisit = binding.root.findViewById(R.id.proposed_next_visit_date_value)

        lifecycleScope.launch {
            proposedDateTextVisit.text = getProposedNextVisitDateAsText()

            val proposedDate = getProposedNextVisitDateAsDate()
            binding.nextVisitDatePickerButton.setOnClickListener {
                ScheduleVisitDatePickerDialog(proposedDate, this@VisitRegisteredSuccessDialog).show(childFragmentManager, "scheduleVisitDatePickerDialog")
            }

            if (proposedDate != null) {
                onDateSelected(proposedDate)
            }
        }

        binding.btnSaveVisit.setOnClickListener {
           lifecycleScope.launch {
               try {
                   validateDate()
                   if (visitDate != null) {
                       createVisitUseCase.createVisit(buildNextVisitObject(participant, Date(visitDate!!.unixMillisLong)))
                       val visitDateAsText = visitDate!!.format(DateFormat.FORMAT_DATE)
                       visitScheduleResultTextView.text = "${getString(R.string.visit_schedule_visit_saved_successfully_label)} $visitDateAsText"
                       visitScheduleResultTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.successDark))
                       nextVisitDateContainerLinearLayout.visibility = View.GONE
                       proposedDateContainerLinearLayout.visibility = View.GONE
                       saveVisitButton.visibility = View.GONE

                       val layoutParams = closeButton.layoutParams as ConstraintLayout.LayoutParams
                       layoutParams.horizontalBias = 0.5f
                       closeButton.layoutParams = layoutParams
                   }
               } catch (ex: Exception) {
                   visitScheduleResultTextView.text = getString(R.string.visit_schedule_visit_failed)
                   visitScheduleResultTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.design_default_color_error))
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

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getProposedNextVisitDateAsDate(): DateTime? {
        val weeksNumberAfterBirthForNextVisit = findWeeksNumberAfterBirthForNextVisit(participant!!.birthDateText)
        val nextVisitDate = weeksNumberAfterBirthForNextVisit?.let { calculateNextVisitDate(participant!!.birthDateText, it) }
        return nextVisitDate?.let { DateTime.fromUnix(it.time) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getProposedNextVisitDateAsText(): String {
        val weeksNumberAfterBirthForNextVisit = findWeeksNumberAfterBirthForNextVisit(participant!!.birthDateText)
        val nextVisitDate = weeksNumberAfterBirthForNextVisit?.let { calculateNextVisitDate(participant!!.birthDateText, it) }
        if (nextVisitDate != null) {
            return DateUtil.convertDateToString(nextVisitDate, DateFormat.FORMAT_DATE.toString())
        }

        return ""
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
    private fun calculateNextVisitDate(birthDateText: String, weeksNumberAfterBirth: Int): Date {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val birthDate = LocalDate.parse(birthDateText, formatter)
        val nextVisitDate = birthDate.plusWeeks(weeksNumberAfterBirth.toLong())
        return Date.from(nextVisitDate.atStartOfDay(ZoneId.of(Constants.UTC_TIME_ZONE_NAME)).toInstant())
    }

    interface VisitRegisteredSuccessDialogListener {
        fun onVisitRegisteredSuccessDialogClosed()
    }
}