package com.jnj.vaccinetracker.visit.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
import com.jnj.vaccinetracker.visit.dialog.DialogVaccineBarcode
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import kotlinx.coroutines.flow.onEach
import java.util.Date

/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitVaccinesFragment : BaseFragment(),
        VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener,
        DialogVaccineBarcode.ConfirmSubstanceListener,
        DialogVaccineBarcode.RemoveSubstanceListener
{

    private companion object {
        private const val TAG_DIALOG_SUCCESS = "successDialog"
    }

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentVisitVaccinesBinding
    private lateinit var adapter: VisitSubstanceItemAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_vaccines, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupClickListeners()
        setupVaccinesRecyclerView()

        return binding.root
    }

    private fun setupVaccinesRecyclerView() {
        adapter = VisitSubstanceItemAdapter(
            mutableListOf(),
            requireActivity().supportFragmentManager,
            requireContext(),
            viewModel,
            viewModel.suggestedSubstancesData.value,
        )
        binding.recyclerViewVaccines.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVaccines.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }
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
        viewModel.selectedSubstancesData.observe(lifecycleOwner) { substances ->
            adapter.updateList(substances)
        }
        viewModel.selectedSubstancesWithBarcodes.observe(lifecycleOwner) { substances ->
            adapter.colorItems(substances)
        }
        viewModel.isSuggesting.observe(lifecycleOwner) {isSuggesting ->
            adapter.setSuggestingMode(isSuggesting)
        }
        viewModel.suggestedSubstancesData.observe(lifecycleOwner) {suggestedSubstances ->
            adapter.setSuggestedSubstances(suggestedSubstances)
        }
    }

    private fun onDosingVisitRegistrationSuccessful() {
        val dosingVisit = viewModel.dosingVisit.value
        VisitRegisteredSuccessDialog.create(viewModel.upcomingVisit.value, viewModel.participant.value, dosingVisit!!.uuid).show(childFragmentManager, TAG_DIALOG_SUCCESS)
    }

    private fun onDosingVisitRegistrationFailed() {
        Snackbar.make(binding.root, R.string.general_label_error, Snackbar.LENGTH_LONG).show()
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
}