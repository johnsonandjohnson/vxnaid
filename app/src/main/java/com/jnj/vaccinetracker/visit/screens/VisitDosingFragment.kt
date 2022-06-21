package com.jnj.vaccinetracker.visit.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.*
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.dialog.DifferentManufacturerExpectedDialog
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
class VisitDosingFragment : BaseFragment(),
    VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener,
    DosingOutOfWindowDialog.DosingOutOfWindowDialogListener,
    DifferentManufacturerExpectedDialog.DifferentManufacturerExpectedListener {

    private companion object {
        private const val REQ_SCAN_BARCODE = 45
        private const val TAG_DIALOG_SUCCESS = "successDialog"
        private const val TAG_DIALOG_DOSING_OUT_OF_WINDOW = "dosingOutOfWindowDialog"
        private const val TAG_DIALOG_DIFFERENT_MANUFACTURER_EXPECTED = "differentManufacturerDialog"
    }

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentVisitDosingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_dosing, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }

        binding.imageButtonBarcodeScanner.setOnClickListener {
            startActivityForResult(ScanBarcodeActivity.create(requireContext()), REQ_SCAN_BARCODE)
        }
        binding.btnSubmit.setOnClickListener {
            submitDosingVisit()
        }
        binding.dropdownManufacturer.setOnItemClickListener { _, _, position, _ ->
            val manufacturerName = viewModel.manufacturerList.get()?.distinct()?.get(position) ?: return@setOnItemClickListener
            viewModel.setSelectedManufacturer(manufacturerName)
        }
        binding.editVialBarcode.doOnTextChanged { s, _, _, _ ->
            if (!s.isNullOrEmpty()) {
                viewModel.matchBarcodeManufacturer(s, resourcesWrapper)
            }
             else{
                viewModel.setSelectedManufacturer("")
                binding.dropdownManufacturer.clearListSelection()
                }
        }

        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.manufacturerList.observe(lifecycleOwner) { manufacturers ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, manufacturers?.distinct().orEmpty())
            binding.dropdownManufacturer.setAdapter(adapter)
        }
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
    }

    private fun submitDosingVisit(overrideOutsideWindowCheck: Boolean = false, overrideManufacturerCheck: Boolean = false) {
        viewModel.submitDosingVisit(
            vialBarcode = binding.editVialBarcode.text.toString(),
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SCAN_BARCODE && resultCode == Activity.RESULT_OK) {
            val barcode = data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE) ?: return
            binding.editVialBarcode.setText(barcode)
            viewModel.matchBarcodeManufacturer(barcode, resourcesWrapper)
        }
    }

    override fun onOutOfWindowDosingConfirmed() {
        submitDosingVisit(overrideOutsideWindowCheck = true)
    }

    override fun onDifferentManufacturerConfirmed() {
        submitDosingVisit(overrideManufacturerCheck = true, overrideOutsideWindowCheck = true)
    }

    override fun onVisitRegisteredSuccessDialogClosed() {
        requireActivity().apply {
            startActivity(SplashActivity.create(this)) // Restart the participant flow
            finishAffinity()
        }
    }

}