package com.jnj.vaccinetracker.visit.dialog

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.data.encryption.SharedPreference
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogVaccineBarcodeBinding
import com.jnj.vaccinetracker.register.dialogs.VaccineDialog
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.jnj.vaccinetracker.visit.screens.VisitDosingFragment

class DialogVaccineBarcode(
   private val substance: SubstanceDataModel
): BaseDialogFragment() {
   private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
   private val scanviewModel: ScanBarcodeViewModel by activityViewModels { viewModelFactory }
   private lateinit var binding: DialogVaccineBarcodeBinding
   private lateinit var btnConfirm: Button
   private lateinit var btnCancel: Button
   private lateinit var barcode: EditText

   private companion object {
      private const val REQ_SCAN_BARCODE = 45
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_vaccine_barcode, container, false)

      initializeViews()
      setOnClickListeners()

      return binding.root
   }

   private fun initializeViews() {
      btnConfirm = binding.btnConfirmSubstance
      btnCancel = binding.btnCancel
      barcode = binding.editVialBarcode
   }

   private fun setOnClickListeners() {
      btnConfirm.setOnClickListener {
         val barcodeText = barcode.text.toString()
         if (barcodeText != "") {
            findParent<ConfirmSubstanceListener>()?.addSubstance(substance, barcodeText)
            dismissAllowingStateLoss()
         }
      }

      btnCancel.setOnClickListener {
         dismissAllowingStateLoss()
      }

      binding.imageButtonBarcodeScanner.setOnClickListener {
         startActivityForResult(
            ScanBarcodeActivity.create(requireContext(), ScanBarcodeActivity.MANUFACTURER),
            REQ_SCAN_BARCODE
         )
      }

      binding.dropdownManufacturer.setOnItemClickListener { _, _, position, _ ->
         val manufacturerName = viewModel.manufacturerList.get()?.distinct()?.get(position)
            ?: return@setOnItemClickListener
         viewModel.setSelectedManufacturer(manufacturerName)
      }

      binding.editVialBarcode.doOnTextChanged { s, _, _, _ ->
         if (!s.isNullOrEmpty()) {
            viewModel.matchBarcodeManufacturer(s, resourcesWrapper)
         } else {
            viewModel.setSelectedManufacturer("")
            binding.dropdownManufacturer.clearListSelection()
         }
      }
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      super.observeViewModel(lifecycleOwner)
      viewModel.manufacturerList.observe(lifecycleOwner) { manufacturers ->
         val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, manufacturers?.distinct().orEmpty())
         binding.dropdownManufacturer.setAdapter(adapter)

         SharedPreference(requireContext()).saveManufracterList(viewModel.getManufactuerList())
         SharedPreference(requireContext()).saveManufracterList(scanviewModel.getManufactuerList())
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      if (requestCode == REQ_SCAN_BARCODE && resultCode == Activity.RESULT_OK) {
         val barcode = data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE) ?: return
         //binding.editVialBarcode.setText(barcode)
         viewModel.matchBarcodeManufacturer(barcode, resourcesWrapper)
      }
   }
   interface ConfirmSubstanceListener {
      fun addSubstance(substance: SubstanceDataModel, barcodeText: String)
   }
}