package com.jnj.vaccinetracker.visit.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentVisitCaptureDataBinding
import com.jnj.vaccinetracker.databinding.ItemVisitHistoryTitleBinding
import com.jnj.vaccinetracker.databinding.ItemVisitPreviousDoseBinding
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import com.jnj.vaccinetracker.visit.zscore.InputFilterMinMax
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitCaptureDataFragment :
    BaseFragment(),
    VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener,
    OtherSubstanceItemAdapter.AddSubstanceValueListener
{

    private companion object {
        private const val TAG_DIALOG_SUCCESS = "successDialog"
    }

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentVisitCaptureDataBinding
    private lateinit var otherSubstancesAdapter: OtherSubstanceItemAdapter

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.previousDosingVisits.observe(lifecycleOwner) { visits ->
            binding.linearLayoutVisitHistory.removeAllViews()
            if (!visits.isNullOrEmpty()) {
                val v = DataBindingUtil.inflate<ItemVisitHistoryTitleBinding>(layoutInflater, R.layout.item_visit_history_title, binding.linearLayoutVisitHistory, true)
                v.title = resourcesWrapper.getString(R.string.visit_vaccination_history)
                visits.forEach { visit ->
                    val view = DataBindingUtil.inflate<ItemVisitPreviousDoseBinding>(layoutInflater, R.layout.item_visit_previous_dose, binding.linearLayoutVisitHistory, true)
                    view.visit = visit
                }
            }
        }
        viewModel.otherSubstancesData.observe(lifecycleOwner) { otherSubstances ->
            otherSubstancesAdapter.updateItemsList(otherSubstances)
        }
        viewModel.checkOtherSubstances.observe(lifecycleOwner) {
            if (viewModel.checkOtherSubstances.value == true) {
                viewModel.isAnyOtherSubstancesEmpty.value = checkIfAnyOtherDataEmpty()
                viewModel.checkOtherSubstances.value = false
            }
        }

        viewModel.checkVisitLocation.observe(lifecycleOwner) {
            if (viewModel.checkVisitLocation.value == true) {
                if (!viewModel.isVisitLocationValid()) {
                    binding.textViewVisitLocationTitle.error = "Please fill before submitting"
                } else {
                    binding.textViewVisitLocationTitle.error = null
                }

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_capture_data, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.radioLocations.setOnCheckedChangeListener { group, checkedId ->
            val selectedRadioButton = binding.root.findViewById<RadioButton>(checkedId)
            val selectedValue = selectedRadioButton?.text.toString()
            viewModel.setVisitLocationValue(selectedValue)
            viewModel.isVisitLocationSelected.value = true
        }

        setOtherSubstancesRecyclerView()
        return binding.root
    }

    private fun setOtherSubstancesRecyclerView() {
        otherSubstancesAdapter = OtherSubstanceItemAdapter(mutableListOf(), this, viewModel.participant.value!!)
        binding.recyclerViewOtherSubstances.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewOtherSubstances.adapter = otherSubstancesAdapter
    }

    override fun addOtherSubstance(substanceName: String, value: String) {
        viewModel.addObsToOtherSubstancesObsMap(substanceName, value)
    }

    override fun onVisitRegisteredSuccessDialogClosed() {
        requireActivity().apply {
            startActivity(SplashActivity.create(this)) // Restart the participant flow
            finishAffinity()
        }
    }

    private fun checkIfAnyOtherDataEmpty(): Boolean {
        return otherSubstancesAdapter.checkIfAnyItemsEmpty(viewModel.selectedOtherSubstances.value, binding.recyclerViewOtherSubstances)
    }

}