package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.NumberPicker
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogSelectVaccineBinding
import com.jnj.vaccinetracker.register.screens.RegisterParticipantAdministeredVaccinesViewModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel

class VaccineDialog : BaseDialogFragment() {
   private lateinit var btnOk: Button
   private lateinit var btnCancel: Button
   private lateinit var dropdown: AutoCompleteTextView
   private lateinit var binding: DialogSelectVaccineBinding
   private lateinit var numberPicker: NumberPicker
   private val viewModelAdministeredVaccines: RegisterParticipantAdministeredVaccinesViewModel by viewModels { viewModelFactory }

   var selectedSubstance: SubstanceDataModel? = null
   var selectedDose: Int? = 1
   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_select_vaccine, container, false)

      initializeViews()
      setupNumberPicker()
      setOnClickListeners()

      return binding.root
   }

   private fun initializeViews() {
      btnOk = binding.btnOk
      btnCancel = binding.btnCancel
      dropdown = binding.dropdownVaccine
      numberPicker = binding.numberPickerDose
   }

   private fun setOnClickListeners() {
      btnOk.setOnClickListener {
         if (selectedSubstance != null && selectedDose != null) {
            findParent<AddVaccineListener>()?.addVaccine(selectedSubstance!!, selectedDose!!)
            dismissAllowingStateLoss()
         }
      }

      btnCancel.setOnClickListener {
         dismissAllowingStateLoss()
      }

      dropdown.setOnItemClickListener { _, _, position, _ ->
         val selectedSubstance = viewModelAdministeredVaccines.substancesData.value?.get(position)
         this.selectedSubstance = selectedSubstance
      }

      numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
         selectedDose = newVal
      }
   }

   private fun setupNumberPicker() {
      numberPicker.minValue = 1
      numberPicker.maxValue = 100
      numberPicker.wrapSelectorWheel = false
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      viewModelAdministeredVaccines.substancesData.observe(lifecycleOwner) { substances ->
         val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            substances?.map { it.conceptName }?.distinct().orEmpty()
         )
         dropdown.setAdapter(adapter)
      }
   }


   interface AddVaccineListener {
      fun addVaccine(vaccine: SubstanceDataModel, dose: Int)
   }
}