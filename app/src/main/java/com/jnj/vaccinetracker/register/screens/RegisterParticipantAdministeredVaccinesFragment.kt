package com.jnj.vaccinetracker.register.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentRegisterParticipantAdministeredVaccinesBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.adapters.SubstanceItemAdapter
import com.jnj.vaccinetracker.register.dialogs.RegisterParticipantSuccessfulDialog
import com.jnj.vaccinetracker.register.dialogs.VaccineDialog
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.flow.onEach

class RegisterParticipantAdministeredVaccinesFragment : BaseFragment(),
   RegisterParticipantSuccessfulDialog.RegisterParticipationCompletionListener,
   VaccineDialog.AddVaccineListener {
   private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
   private val viewModel: RegisterParticipantAdministeredVaccinesViewModel by viewModels { viewModelFactory }
   private lateinit var binding: FragmentRegisterParticipantAdministeredVaccinesBinding
   private lateinit var adapter: SubstanceItemAdapter
   private var participant: ParticipantSummaryUiModel? = null

   companion object {
      private const val TAG_VACCINE_PICKER = "vaccinePicker"
      private const val TAG_SUCCESS_DIALOG = "successDialog"

      fun create(participant: ParticipantSummaryUiModel): RegisterParticipantAdministeredVaccinesFragment {
         val fragment = RegisterParticipantAdministeredVaccinesFragment()
         val args = Bundle().apply {
            putParcelable(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT, participant)
         }
         fragment.arguments = args
         return fragment
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      arguments?.let {
         participant = it.getParcelable(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT)
      }
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(
         inflater,
         R.layout.fragment_register_participant_administered_vaccines,
         container,
         false
      )
      binding.viewModel = viewModel
      viewModel.setArguments(participant)
      binding.lifecycleOwner = viewLifecycleOwner
      binding.flowViewModel = flowViewModel
      binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }

      setupRecyclerView()
      setupClickListeners()

      return binding.root
   }

   private fun setupRecyclerView() {
      adapter = SubstanceItemAdapter(mutableListOf(), viewModel)
      binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
      binding.recyclerView.adapter = adapter
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      observeViewModelEvents(lifecycleOwner)

      viewModel.selectedSubstances.observe(lifecycleOwner){substanceItems ->
         adapter.updateList(substanceItems)
         if (viewModel.selectedSubstances.value?.isEmpty() == true || viewModel.selectedSubstances.value == null) {
            binding.recyclerView.visibility = View.GONE
            binding.textViewNoVaccines.visibility = View.VISIBLE
         } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.textViewNoVaccines.visibility = View.GONE
         }
      }
   }

   private fun observeViewModelEvents(lifecycleOwner: LifecycleOwner) = viewModel.apply {
      registerVaccinesSuccessEvents
         .asFlow()
         .onEach { participant ->
            RegisterParticipantSuccessfulDialog.create(participant)
               .show(
                  childFragmentManager,
                  TAG_SUCCESS_DIALOG
               )
         }.launchIn(lifecycleOwner)
   }

   private fun setupClickListeners() {
      binding.btnAddVaccine.setOnClickListener {
         val allSubstances = viewModel.substancesData.value.orEmpty()
         val selectedSubstances = viewModel.selectedSubstances.value.orEmpty()

         val filteredSubstances = allSubstances.filter { substance ->
            selectedSubstances.none { selected -> selected.conceptName == substance.conceptName }
         }

         VaccineDialog(filteredSubstances).show(childFragmentManager, TAG_VACCINE_PICKER)
      }
      binding.btnSubmit.setOnClickListener {
         submitVaccineRegistration()
      }
   }

   private fun submitVaccineRegistration() {
      viewModel.submitVaccineRegistration()
   }

   override fun continueWithParticipantVisit(participant: ParticipantSummaryUiModel) {
      (requireActivity() as BaseActivity).run {
         setResult(
            Activity.RESULT_OK,
            Intent().putExtra(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT, participant)
         )
         finish()
      }
   }

   override fun finishParticipantFlow() {
      (requireActivity() as BaseActivity).run {
         setResult(Activity.RESULT_OK)
         finish()
      }
   }

   override fun addVaccine(vaccine: SubstanceDataModel) {
      viewModel.addSelectedSubstance(vaccine)
   }

   override fun addVaccineDate(conceptName: String, dateValue: DateTime) {
      viewModel.addVaccineDate(conceptName, dateValue.format(DateFormat.FORMAT_DATE))
   }
}