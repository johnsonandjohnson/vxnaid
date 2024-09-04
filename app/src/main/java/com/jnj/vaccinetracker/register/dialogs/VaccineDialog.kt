package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogSelectVaccineBinding
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime

class VaccineDialog(
   private val substanceData: List<SubstanceDataModel>
) : BaseDialogFragment() {
   private lateinit var btnAdd: Button
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
      btnAdd = binding.btnAdd
      btnCancel = binding.btnCancel
      dropdown = binding.dropdownVaccine
   }

   private fun setOnClickListeners() {
      btnAdd.setOnClickListener {
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
         substanceData.map { it.label }.distinct()
      )
      dropdown.setAdapter(adapter)
   }

   interface AddVaccineListener {
      fun addVaccine(vaccine: SubstanceDataModel)
   }
}