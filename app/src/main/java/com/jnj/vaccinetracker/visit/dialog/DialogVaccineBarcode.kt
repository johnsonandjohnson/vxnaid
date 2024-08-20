package com.jnj.vaccinetracker.visit.dialog

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel

class DialogVaccineBarcode(
   private val substance: SubstanceDataModel,
   private val barcode: String? = null,
   private val manufacturerName: String? = null,
): BaseDialogFragment() {
   private val scanViewModel: ScanBarcodeViewModel by activityViewModels { viewModelFactory }
   private lateinit var binding: DialogVaccineBarcodeBinding
   private lateinit var btnConfirm: Button
   private lateinit var btnCancel: Button
   private lateinit var barcodeEditText: EditText
   private lateinit var manufacturerDropdown: AutoCompleteTextView
   private var selectedManufacturerName: String = ""

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

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)
      initializeData()
   }

   private fun initializeData() {
      setBarcode(barcode)
      scanViewModel.selectedManufacturerName.value = manufacturerName
   }

   private fun initializeViews() {
      btnConfirm = binding.btnConfirmSubstance
      btnCancel = binding.btnCancel
      barcodeEditText = binding.editVialBarcode
      manufacturerDropdown = binding.dropdownManufacturer
   }

   private fun setOnClickListeners() {
      btnConfirm.setOnClickListener {
         val barcodeText = barcodeEditText.text.toString()
         if (barcodeText.isNotEmpty() || selectedManufacturerName.isNotEmpty()) {
            findParent<ConfirmSubstanceListener>()?.addSubstance(substance, barcodeText, selectedManufacturerName)
            dismissAllowingStateLoss()
         } else {
            findParent<RemoveSubstanceListener>()?.removeSubstance(substance)
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
         val manufacturerName = scanViewModel.manufacturerNames.get()?.distinct()?.get(position)
            ?: return@setOnItemClickListener
         updateSelectedManufacturerName(manufacturerName)
      }

      binding.editVialBarcode.doOnTextChanged { s, _, _, _ ->
         if (!s.isNullOrEmpty()) {
            scanViewModel.matchBarcodeManufacturer(s)
         } else {
            updateSelectedManufacturerName("")
            binding.dropdownManufacturer.clearListSelection()
         }
      }
   }

   private fun setBarcode(barcode: String?) {
      this.barcodeEditText.setText(barcode ?: "")
   }

   private fun updateSelectedManufacturerName(name: String?) {
      if (name == selectedManufacturerName) return
      val text = name ?: ""
      selectedManufacturerName = text
      manufacturerDropdown.post {
         manufacturerDropdown.setText(selectedManufacturerName)
      }
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      super.observeViewModel(lifecycleOwner)
      scanViewModel.manufacturerNames.observe(lifecycleOwner) { manufacturers ->
         val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, manufacturers?.distinct().orEmpty())
         binding.dropdownManufacturer.setAdapter(adapter)

         SharedPreference(requireContext()).saveManufracterList(scanViewModel.getManufacturers())
      }
      scanViewModel.selectedManufacturerName.observe(lifecycleOwner) {name ->
         updateSelectedManufacturerName(name)
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      if (requestCode == REQ_SCAN_BARCODE && resultCode == Activity.RESULT_OK) {
         val barcode = data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE) ?: return
         setBarcode(barcode)
         scanViewModel.matchBarcodeManufacturer(barcode)
      }
   }

   interface ConfirmSubstanceListener {
      fun addSubstance(substance: SubstanceDataModel, barcodeText: String, manufacturerName: String)
   }
   interface RemoveSubstanceListener {
      fun removeSubstance(substance: SubstanceDataModel)
   }
}