package com.jnj.vaccinetracker.register.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.helpers.dpToPx
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentRegisterHistoricalVisitsBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.RegisterParticipantSuccessfulDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class RegisterParticipantHistoricalDataFragment : BaseFragment(),
   RegisterParticipantSuccessfulDialog.RegisterParticipationCompletionListener {

   private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
   private val viewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels { viewModelFactory }
   private lateinit var binding: FragmentRegisterHistoricalVisitsBinding

   companion object {
      private const val TAG_SUCCESS_DIALOG = "successDialog"
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(
         inflater,
         R.layout.fragment_register_historical_visits,
         container,
         false
      )
      binding.apply {
         viewModel = this@RegisterParticipantHistoricalDataFragment.viewModel
         lifecycleOwner = viewLifecycleOwner
         flowViewModel = this@RegisterParticipantHistoricalDataFragment.flowViewModel
      }
      viewModel.setArguments(flowViewModel.participant.value)
      binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }

      setupClickListeners()
      setupButtons()

      return binding.root
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      viewModel.visitTypesData.observe(lifecycleOwner) { _ ->
         setupButtons()
      }
      observeViewModelEvents(lifecycleOwner)
   }

   private fun observeViewModelEvents(lifecycleOwner: LifecycleOwner) {
      viewModel.registerVaccinesSuccessEvents
         .asFlow()
         .onEach { participant ->
            RegisterParticipantSuccessfulDialog.create(participant)
               .show(childFragmentManager, TAG_SUCCESS_DIALOG)
         }
         .launchIn(lifecycleOwner.lifecycleScope)
   }

   private fun setupClickListeners() {
      binding.btnSubmit.setOnClickListener {
         submitVaccineRegistration()
      }
   }

   private fun submitVaccineRegistration() {
      lifecycleScope.launch {
         try {
            viewModel.submitVaccineRegistration()
         } catch (e: Exception) {
            e.printStackTrace()
         }
      }
   }

   private fun setupButtons() {
      binding.buttonGrid.apply {
         removeAllViews()
         Constants.VISIT_TYPES.forEach { visitTypeName ->
            addView(createButton(visitTypeName))
         }
      }
   }

   private fun createButton(name: String): Button {
      return Button(requireContext()).apply {
         layoutParams = createButtonLayoutParams()
         text = name
         setTextAppearance(R.style.ButtonTextStyling)
         setTextColor(ContextCompat.getColorStateList(context, R.color.colorTextOnPrimary))
         background = ContextCompat.getDrawable(context, R.drawable.rounded_button_30dp)
         backgroundTintList = getButtonBackgroundTint(name)
         setOnClickListener { onButtonClicked(name) }
      }
   }

   private fun createButtonLayoutParams(): FrameLayout.LayoutParams {
      val size = 200.dpToPx
      return FrameLayout.LayoutParams(size, size).apply {
         setMargins(32.dpToPx, 32.dpToPx, 32.dpToPx, 32.dpToPx)
      }
   }

   private fun getButtonBackgroundTint(visitTypeName: String) = if (viewModel.visitTypesData.value?.containsKey(visitTypeName) == true) {
      ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
   } else {
      ContextCompat.getColorStateList(requireContext(), R.color.colorTextOnLight)
   }

   private fun onButtonClicked(name: String) {
      flowViewModel.openHistoricalDataForVisitType(name)
   }

   override fun continueWithParticipantVisit(participant: ParticipantSummaryUiModel) {
      finishActivityWithResult(participant)
   }

   override fun finishParticipantFlow() {
      finishActivityWithResult()
   }

   private fun finishActivityWithResult(participant: ParticipantSummaryUiModel? = null) {
      (requireActivity() as BaseActivity).run {
         setResult(
            Activity.RESULT_OK,
            Intent().putExtra(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT, participant)
         )
         finish()
      }
   }
}
