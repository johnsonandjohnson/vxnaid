package com.jnj.vaccinetracker.register.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.barcode.formatParticipantId
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentRegisterParticipantParticipantDetailsBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.*
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
@SuppressWarnings("TooManyFunctions")
class RegisterParticipantParticipantDetailsFragment : BaseFragment(),
    HomeLocationPickerDialog.HomeLocationPickerListener,
    RegisterParticipantSuccessfulDialog.RegisterParticipationCompletionListener,
    RegisterParticipantConfirmNoTelephoneDialog.RegisterParticipationNoTelephoneConfirmationListener {

    private companion object {
        private const val TAG_HOME_LOCATION_PICKER = "homeLocationPicker"
        private const val TAG_SUCCESS_DIALOG = "successDialog"
        private const val TAG_NO_PHONE_DIALOG = "confirmNoPhoneDialog"
        private const val TAG_NO_MATCHING_ID = "noMatchingIdDialog"
        private const val REQ_BARCODE = 213
    }

    private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
    private val viewModel: RegisterParticipantParticipantDetailsViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentRegisterParticipantParticipantDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register_participant_participant_details, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.flowViewModel = flowViewModel
        binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }

        setupPhoneInput()
        setupDropdowns()
        setupClickListeners()
        setupInputListeners()

        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.defaultPhoneCountryCode.observe(lifecycleOwner) { countryCode ->
            if (countryCode != null) {
                binding.countryCodePickerPhone.setDefaultCountryUsingNameCode(countryCode)
                if (flowViewModel.countryCode.get().isNullOrEmpty())
                    binding.countryCodePickerPhone.resetToDefaultCountry()
            }
        }
        flowViewModel.countryCode.observe(lifecycleOwner) { countryCode ->
            if (countryCode != null) {
                binding.countryCodePickerPhone.setCountryForPhoneCode(countryCode.toInt())
            }
        }
        viewModel.vaccineNames.observe(lifecycleOwner) { vaccineNames ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, vaccineNames.orEmpty().map { it.display })
            binding.dropdownVaccine.setAdapter(adapter)
        }
        viewModel.languages.observe(lifecycleOwner) { languages ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, languages.orEmpty().map { it.display })
            binding.dropdownLanguage.setAdapter(adapter)
        }
        viewModel.genderValidationMessage.observe(lifecycleOwner) { genderValidationMessage ->
            logDebug("validate gender" + genderValidationMessage)

            binding.genderError.requestFocus()
            binding.genderError.error = genderValidationMessage
        }
        observeViewModelEvents(lifecycleOwner)
    }

    private fun observeViewModelEvents(lifecycleOwner: LifecycleOwner) = viewModel.apply {
        registerSuccessEvents
            .asFlow()
            .onEach { participant ->
                RegisterParticipantSuccessfulDialog.create(participant)
                    .show(childFragmentManager, TAG_SUCCESS_DIALOG)
            }.launchIn(lifecycleOwner)
        registerNoPhoneEvents
            .asFlow()
            .onEach {
                RegisterParticipantConfirmNoTelephoneDialog().show(childFragmentManager, TAG_NO_PHONE_DIALOG)
            }.launchIn(lifecycleOwner)
        registerNoMatchingIdEvents
            .asFlow()
            .onEach {
                RegisterParticipantIdNotMatchingDialog().show(childFragmentManager, TAG_NO_MATCHING_ID)
            }.launchIn(lifecycleOwner)
        registerFailedEvents
            .asFlow()
            .onEach { errorMessage ->
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
            }.launchIn(lifecycleOwner)
    }

    private fun setupInputListeners() {

        binding.editConfirmParticipantId.doAfterTextChanged {
            val text = binding.editConfirmParticipantId.text.toString()
            val formattedText = formatParticipantId(text)
            if (formattedText != text) {
                binding.editConfirmParticipantId.setTextKeepSelection(formattedText)
            }
            viewModel.setConfirmParticipantId(formattedText)
        }

        binding.editParticipantId.doAfterTextChanged {
            val text = binding.editParticipantId.text.toString()
            val formattedText = formatParticipantId(text)
            if (formattedText != text) {
                binding.editParticipantId.setTextKeepSelection(formattedText)
            }
            viewModel.setParticipantId(formattedText)
        }

        binding.editTelephone.doAfterTextChanged {
            viewModel.setPhone(it?.toString().orEmpty())
        }

        binding.editYearOfBirth.doAfterTextChanged {
            viewModel.setYearOfBirth(it?.toString().orEmpty())
        }
    }

    private fun setupPhoneInput() {
        binding.countryCodePickerPhone.registerCarrierNumberEditText(binding.editTelephone)
        binding.countryCodePickerPhone.setOnCountryChangeListener {
            viewModel.setPhoneCountryCode(binding.countryCodePickerPhone.selectedCountryCode)
        }
        val countryCode = binding.countryCodePickerPhone.selectedCountryCode
        viewModel.setPhoneCountryCode(countryCode)
    }

    private fun setupClickListeners() {
        binding.btnSetHomeLocation.setOnClickListener {
            HomeLocationPickerDialog().show(childFragmentManager, TAG_HOME_LOCATION_PICKER)
        }
        binding.btnSubmit.setOnClickListener {
            submitRegistration()
        }
        binding.rbGenderMale.setOnClickListener {
            updateGender()
        }
        binding.rbGenderFemale.setOnClickListener {
            updateGender()
        }
        binding.rbGenderOther.setOnClickListener {
            updateGender()
        }
        binding.genderError.setOnClickListener {
            it.requestFocus()
        }
        binding.btnScanParticipantId.setOnClickListener {
            startActivityForResult(ScanBarcodeActivity.create(requireContext()), REQ_BARCODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_BARCODE && resultCode == Activity.RESULT_OK) {
            val participantIdBarcode = data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE) ?: return
            viewModel.onParticipantIdScanned(participantIdBarcode)
        }
    }

    private fun setupDropdowns() {
        binding.dropdownVaccine.setOnItemClickListener { _, _, position, _ ->
            val vaccineName = viewModel.vaccineNames.value?.get(position) ?: return@setOnItemClickListener
            viewModel.setSelectedVaccine(vaccineName)
        }
        binding.dropdownLanguage.setOnItemClickListener { _, _, position, _ ->
            val languageName = viewModel.languages.value?.get(position) ?: return@setOnItemClickListener
            viewModel.setSelectedLanguage(languageName)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setArguments(
            RegisterParticipantParticipantDetailsViewModel.Args(
                participantId = flowViewModel.participantId.value,
                isManualSetParticipantID = flowViewModel.isManualEnteredId.value,
                leftEyeScanned = flowViewModel.leftEyeScanned.value,
                rightEyeScanned = flowViewModel.rightEyeScanned.value,
                phoneNumber = flowViewModel.phoneNumber.value,
            )
        )
    }

    private fun submitRegistration() {
        viewModel.submitRegistration(flowViewModel.participantPicture.value)
    }

    private fun updateGender() {
        val gender = when {
            binding.rbGenderMale.isChecked -> Gender.MALE
            binding.rbGenderFemale.isChecked -> Gender.FEMALE
            binding.rbGenderOther.isChecked -> Gender.OTHER
            else -> return
        }
        viewModel.setGender(gender)
    }

    override fun onHomeLocationPicked(address: HomeLocationPickerViewModel.AddressUiModel) {
        viewModel.setHomeLocation(address.addressMap, address.stringRepresentation)
    }

    override fun continueWithParticipantVisit(participant: ParticipantSummaryUiModel) {
        (requireActivity() as BaseActivity).run {
            setResult(Activity.RESULT_OK, Intent().putExtra(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT, participant))
            finish()
        }
    }

    override fun finishParticipantFlow() {
        (requireActivity() as BaseActivity).run {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun confirmNoTelephone() {
        viewModel.canSkipPhone = true
        submitRegistration()
    }

}