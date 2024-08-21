package com.jnj.vaccinetracker.visit.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.*
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.adapters.VisitSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.dialog.DialogScheduleMissingSubstances
import com.jnj.vaccinetracker.visit.dialog.DialogVaccineBarcode
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.jnj.vaccinetracker.visit.zscore.InputFilterMinMax
import kotlinx.coroutines.flow.onEach
import java.util.Date

/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitDosingFragment : BaseFragment(),
        VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener,
        DosingOutOfWindowDialog.DosingOutOfWindowDialogListener,
        DialogVaccineBarcode.ConfirmSubstanceListener,
        DialogScheduleMissingSubstances.DialogScheduleMissingSubstancesListener,
        OtherSubstanceItemAdapter.AddSubstanceValueListener,
        DialogVaccineBarcode.RemoveSubstanceListener
{

    private companion object {
        private const val TAG_DIALOG_SUCCESS = "successDialog"
        private const val TAG_DIALOG_DOSING_OUT_OF_WINDOW = "dosingOutOfWindowDialog"
        private const val TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES = "scheduleMissingSubstances"
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
    private lateinit var otherSubstancesAdapter: OtherSubstanceItemAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_dosing, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupFilters()
        setupClickListeners()
        setupInputListeners()
        setupVaccinesRecyclerView()
        setOtherSubstancesRecyclerView()

        return binding.root
    }

    private fun setupFilters() {
        binding.editTextWeightInput.filters = arrayOf(InputFilterMinMax(MIN_WEIGHT, MAX_WEIGHT))
        binding.editTextHeightInput.filters = arrayOf(InputFilterMinMax(MIN_HEIGHT, MAX_HEIGHT))
        binding.editTextMuacInput.filters = arrayOf(InputFilterMinMax(MIN_MUAC, MAX_MUAC))
    }

    private fun setupVaccinesRecyclerView() {
        adapter = VisitSubstanceItemAdapter(mutableListOf(), requireActivity().supportFragmentManager, requireContext())
        binding.recyclerViewVaccines.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVaccines.adapter = adapter
    }

    private fun setOtherSubstancesRecyclerView() {
        otherSubstancesAdapter = OtherSubstanceItemAdapter(mutableListOf(), this)
        binding.recyclerViewOtherSubstances.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewOtherSubstances.adapter = otherSubstancesAdapter
    }

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
        viewModel.otherSubstancesData.observe(lifecycleOwner) { otherSubstances ->
            otherSubstancesAdapter.updateItemsList(otherSubstances)
        }
        viewModel.selectedSubstancesWithBarcodes.observe(lifecycleOwner) { substances ->
            adapter.colorItems(substances)
        }
    }

    private fun submitDosingVisit(overrideOutsideWindowCheck: Boolean = false,
                                  newVisitDate: Date? = null) {
        viewModel.submitDosingVisit(
                outsideTimeWindowConfirmationListener = ::showOutsideTimeWindowConfirmationDialog,
                missingSubstancesListener = ::showScheduleMissingSubstancesDialog,
                overrideOutsideTimeWindowCheck = overrideOutsideWindowCheck,
                newVisitDate = newVisitDate
        )
    }

    private fun onDosingVisitRegistrationSuccessful() {
        VisitRegisteredSuccessDialog.create(viewModel.upcomingVisit.value, viewModel.participant.value).show(childFragmentManager, TAG_DIALOG_SUCCESS)
    }

    private fun showOutsideTimeWindowConfirmationDialog() {
        DosingOutOfWindowDialog().show(childFragmentManager, TAG_DIALOG_DOSING_OUT_OF_WINDOW)
    }

    private fun showScheduleMissingSubstancesDialog(substances: List<String>) {
        DialogScheduleMissingSubstances(substances).show(childFragmentManager, TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES)
    }

    private fun onDosingVisitRegistrationFailed() {
        Snackbar.make(binding.root, R.string.general_label_error, Snackbar.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOutOfWindowDosingConfirmed() {
        submitDosingVisit(overrideOutsideWindowCheck = true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDialogScheduleMissingSubstancesConfirmed(date: Date) {
        submitDosingVisit(newVisitDate = date)
    }

    override fun onVisitRegisteredSuccessDialogClosed() {
        requireActivity().apply {
            startActivity(SplashActivity.create(this)) // Restart the participant flow
            finishAffinity()
        }
    }

    override fun addSubstance(substance: SubstanceDataModel, barcodeText: String, manufacturerName: String) {
        viewModel.addObsToObsMap(substance.conceptName, barcodeText, manufacturerName)
    }

    override fun removeSubstance(substance: SubstanceDataModel) {
        viewModel.removeObsFromMap(substance.conceptName)
    }

    override fun addOtherSubstance(substanceName: String, value: String) {
        viewModel.addObsToOtherSubstancesObsMap(substanceName, value)
    }
}