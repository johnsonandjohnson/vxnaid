package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogSelectVaccineBinding
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel

class VaccineDialog(
   private val substanceData: List<SubstanceDataModel>
) : BaseDialogFragment() {
   private lateinit var btnOk: Button
   private lateinit var btnCancel: Button
   private lateinit var dropdown: AutoCompleteTextView
   private lateinit var binding: DialogSelectVaccineBinding

   var selectedSubstance: SubstanceDataModel? = null
   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_select_vaccine, container, false)

      initializeViews()
      setOnClickListeners()
      setupDropdown()

      return binding.root
   }

   private fun initializeViews() {
      btnOk = binding.btnOk
      btnCancel = binding.btnCancel
      dropdown = binding.dropdownVaccine
   }

   private fun setOnClickListeners() {
      btnOk.setOnClickListener {
         if (selectedSubstance != null) {
            findParent<AddVaccineListener>()?.addVaccine(selectedSubstance!!)
            dismissAllowingStateLoss()
         }
      }

      btnCancel.setOnClickListener {
         dismissAllowingStateLoss()
      }

      dropdown.setOnItemClickListener { _, _, position, _ ->
         substanceData.let { substances ->
            if (substances.isNotEmpty() && position >= 0 && position < substances.size) {
               val selectedSubstance = substances[position]
               this.selectedSubstance = selectedSubstance
            } else {
               this.selectedSubstance = null
            }
         }
      }
   }

   private fun setupDropdown() {
      val adapter = ArrayAdapter(
         requireContext(),
         R.layout.item_dropdown,
         substanceData.map { it.conceptName }.distinct()
      )
      dropdown.setAdapter(adapter)
   }


   interface AddVaccineListener {
      fun addVaccine(vaccine: SubstanceDataModel)
   }
}