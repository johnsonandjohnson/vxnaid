package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowPhoneNumberBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowNoTelephoneDialog
import kotlinx.coroutines.flow.onEach

class ParticipantFlowPhoneNumberFragment : BaseFragment(), ParticipantFlowNoTelephoneDialog.ParticipantFlowNoPhoneConfirmationListener {

    private companion object {
        private const val TAG_NO_PHONE_DIALOG = "confirmSkipPhoneDialog"
    }

    private val viewModel: ParticipantFlowPhoneNumberViewModel by viewModels { viewModelFactory }
    private val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowPhoneNumberBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_phone_number, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupPhoneInput()

        // If PHONE is in the workflowItems, this fragment can be skipped only if not mandatory
        viewModel.canSkip.set(!flowViewModel.workflowItems.first { it == ParticipantFlowViewModel.WorkflowItem.PHONE }.mandatory)

        binding.editTelephone.doAfterTextChanged {
            viewModel.validateInput(it?.toString())
        }

        binding.btnSkip.setOnClickListener {
            viewModel.onSkipButtonClick()
        }
        binding.btnSubmit.setOnClickListener {
            if (viewModel.canSubmit.get()) {
                val phoneNumber = viewModel.phone.get()
                val countryCode = viewModel.phoneCountryCode.get()
                flowViewModel.confirmPhone(countryCode, phoneNumber)
                viewModel.setPrefCountryCode(countryCode.toString())
            }
        }
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return binding.root
    }


    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.defaultPhoneCountryCode.observe(lifecycleOwner) { countryCode ->
            if (countryCode != null) {
                binding.countryCodePickerPhone.setDefaultCountryUsingNameCode(countryCode)
            }
        }
        flowViewModel.participantPhone.observe(lifecycleOwner) { phone ->
            if (binding.editTelephone.text.isEmpty()) {
                flowViewModel.phoneCountryCode.get()?.let { binding.countryCodePickerPhone.setCountryForPhoneCode(it.toInt()) }
                binding.editTelephone.setText(phone)
            }
        }

        viewModel.confirmWithNullValuesEvent.asFlow().onEach {
            flowViewModel.confirmPhone(null, null)
        }.launchIn(lifecycleOwner)

        viewModel.showNoPhoneWarningEvent.asFlow().onEach {
            ParticipantFlowNoTelephoneDialog().show(childFragmentManager, TAG_NO_PHONE_DIALOG)
        }.launchIn(lifecycleOwner)
    }

    private fun setupPhoneInput() {
        binding.countryCodePickerPhone.registerCarrierNumberEditText(binding.editTelephone)
        binding.countryCodePickerPhone.setOnCountryChangeListener {
            val selectedCountryCode = binding.countryCodePickerPhone.selectedCountryCode
            logInfo("country code picked: $selectedCountryCode")
            viewModel.setPhoneCountryCode(selectedCountryCode)
        }
        if(viewModel.prefCountryCode.get()!=null)
            binding.countryCodePickerPhone.setCountryForPhoneCode(viewModel.prefCountryCode.get()!!.toInt())
        val countryCode = binding.countryCodePickerPhone.selectedCountryCode
        viewModel.setPhoneCountryCode(countryCode)
    }

    override fun confirmNoTelephone() {
        viewModel.onConfirmNoPhoneClick()
    }

}