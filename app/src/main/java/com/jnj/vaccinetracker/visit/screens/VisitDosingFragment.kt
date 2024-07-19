package com.jnj.vaccinetracker.visit.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.data.encryption.SharedPreference
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.*
import com.jnj.vaccinetracker.register.adapters.SubstanceItemAdapter
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.adapters.VisitSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.dialog.DialogVaccineBarcode
import com.jnj.vaccinetracker.visit.dialog.DifferentManufacturerExpectedDialog
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.jnj.vaccinetracker.visit.zscore.InputFilterMinMax
import kotlinx.coroutines.flow.onEach


/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
class VisitDosingFragment : BaseFragment(),
        VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener,
        DosingOutOfWindowDialog.DosingOutOfWindowDialogListener,
        DifferentManufacturerExpectedDialog.DifferentManufacturerExpectedListener,
        DialogVaccineBarcode.ConfirmSubstanceListener
{

    private companion object {
        private const val TAG_DIALOG_SUCCESS = "successDialog"
        private const val TAG_DIALOG_DOSING_OUT_OF_WINDOW = "dosingOutOfWindowDialog"
        private const val TAG_DIALOG_DIFFERENT_MANUFACTURER_EXPECTED = "differentManufacturerDialog"
        private const val MIN_WEIGHT = 1
        private const val MAX_WEIGHT = 300
        private const val MIN_HEIGHT = 1
        private const val MAX_HEIGHT = 300
        private const val MIN_MUAC = 1
        private const val MAX_MUAC = 300
    }

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentVisitDosingBinding
    private lateinit var adapter: VisitSubstanceItemAdapter


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_dosing, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupFilters()
        setupClickListeners()
        setupInputListeners()
        setupRecyclerView()


        return binding.root
    }

    private fun setupFilters() {
        binding.editTextWeightInput.filters = arrayOf(InputFilterMinMax(MIN_WEIGHT, MAX_WEIGHT))
        binding.editTextHeightInput.filters = arrayOf(InputFilterMinMax(MIN_HEIGHT, MAX_HEIGHT))
        binding.editTextMuacInput.filters = arrayOf(InputFilterMinMax(MIN_MUAC, MAX_MUAC))
    }

    private fun setupRecyclerView() {
        adapter = VisitSubstanceItemAdapter(mutableListOf(), requireActivity().supportFragmentManager, requireContext())
        binding.recyclerViewVaccines.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVaccines.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupClickListeners() {
        binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }
        binding.btnSubmit.setOnClickListener {
            submitDosingVisit()
        }
        binding.checkBoxIsOedema.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsOedema(isChecked)
        }
    }

    private fun setupInputListeners() {
        binding.editTextWeightInput.doOnTextChanged { s, _, _, _ ->
            s.toString().toIntOrNull().let { newVal ->
                viewModel.setWeight(newVal)
            }
        }
        binding.editTextHeightInput.doOnTextChanged { s, _, _, _ ->
            s.toString().toIntOrNull().let { newVal ->
                viewModel.setHeight(newVal)
            }
        }
        binding.editTextMuacInput.doOnTextChanged { s, _, _, _ ->
            s.toString().toIntOrNull().let { newVal ->
                viewModel.setMuac(newVal)
            }
        }
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.visitEvents
                .asFlow()
                .onEach { success ->
                    if (success)
                        onDosingVisitRegistrationSuccessful()
                    else
                        onDosingVisitRegistrationFailed()
                }.launchIn(lifecycleOwner)

        viewModel.previousDosingVisits.observe(lifecycleOwner) { visits ->
            binding.linearLayoutVisitHistory.removeAllViews()
            if (visits != null && visits.isNotEmpty()) {

                val v = DataBindingUtil.inflate<ItemVisitHistoryTitleBinding>(layoutInflater, R.layout.item_visit_history_title, binding.linearLayoutVisitHistory, true)
                v.title = resourcesWrapper.getString(R.string.visit_dosing_title_history)

                visits.forEach { visit ->
                    val view = DataBindingUtil.inflate<ItemVisitPreviousDoseBinding>(layoutInflater, R.layout.item_visit_previous_dose, binding.linearLayoutVisitHistory, true)
                    view.visit = visit
                }
            }
        }
        viewModel.zScoreNutritionTextColor.observe(lifecycleOwner) {
            binding.textViewZScoreNutrition.setTextColor(it)
        }
        viewModel.zScoreMuacTextColor.observe(lifecycleOwner) {
            binding.textViewZScoreMuacValue.setTextColor(it)
        }
        viewModel.substancesData.observe(lifecycleOwner) { substances ->
            adapter.updateList(substances)
        }
        viewModel.selectedSubstancesAndBarcodes.observe(lifecycleOwner) { substances ->
            adapter.colorItems(substances)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitDosingVisit(overrideOutsideWindowCheck: Boolean = false, overrideManufacturerCheck: Boolean = false) {
        viewModel.submitDosingVisit(
                vialBarcode = "",
                outsideTimeWindowConfirmationListener = ::showOutsideTimeWindowConfirmationDialog,
                incorrectManufacturerListener = ::showDifferentManufacturerDialog,
                overrideOutsideTimeWindowCheck = overrideOutsideWindowCheck,
                overrideManufacturerCheck = overrideManufacturerCheck
        )
    }

    private fun onDosingVisitRegistrationSuccessful() {
        VisitRegisteredSuccessDialog.create(viewModel.upcomingVisit.value).show(childFragmentManager, TAG_DIALOG_SUCCESS)
    }

    private fun showOutsideTimeWindowConfirmationDialog() {
        DosingOutOfWindowDialog().show(childFragmentManager, TAG_DIALOG_DOSING_OUT_OF_WINDOW)
    }

    private fun showDifferentManufacturerDialog() {
        DifferentManufacturerExpectedDialog().show(childFragmentManager, TAG_DIALOG_DIFFERENT_MANUFACTURER_EXPECTED)
    }

    private fun onDosingVisitRegistrationFailed() {
        Snackbar.make(binding.root, R.string.general_label_error, Snackbar.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOutOfWindowDosingConfirmed() {
        submitDosingVisit(overrideOutsideWindowCheck = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDifferentManufacturerConfirmed() {
        submitDosingVisit(overrideManufacturerCheck = true, overrideOutsideWindowCheck = true)
    }

    override fun onVisitRegisteredSuccessDialogClosed() {
        requireActivity().apply {
            startActivity(SplashActivity.create(this)) // Restart the participant flow
            finishAffinity()
        }
    }

    override fun addSubstance(substance: SubstanceDataModel, barcodeText: String) {
        viewModel.setSelectedSubstances(substance, barcodeText)
    }

}