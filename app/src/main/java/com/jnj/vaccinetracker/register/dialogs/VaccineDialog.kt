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
   private val substanceData: List<SubstanceDataModel>,
   private val withDate: Boolean = true
) : BaseDialogFragment(), ScheduleVisitDatePickerDialog.OnDateSelectedListener {
   private lateinit var btnAdd: Button
   private lateinit var btnCancel: Button
   private lateinit var dropdown: AutoCompleteTextView
   private lateinit var binding: DialogSelectVaccineBinding
   private lateinit var vaccineDateButton: Button
   private lateinit var vaccineDateTextView: TextView
   private lateinit var dateLayout: LinearLayout
   var selectedSubstance: SubstanceDataModel? = null
   private var vaccineDate: DateTime? = null

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
      vaccineDateButton = binding.vaccineDateDatePickerButton
      vaccineDateTextView = binding.vaccineDateValue
      dateLayout = binding.vaccineDateLinearLayout

      if (!withDate) {
         dateLayout.visibility = View.GONE
      }
   }

   private fun setOnClickListeners() {
      btnAdd.setOnClickListener {
         validateDate()
         if (selectedSubstance != null || (vaccineDate != null && withDate)) {
            selectedSubstance!!.obsDate = vaccineDate?.format(DateFormat.FORMAT_DATE)
            findParent<AddVaccineListener>()?.addVaccine(selectedSubstance!!)
            if (withDate) {
               findParent<AddVaccineListener>()?.addVaccineDate(
                  selectedSubstance!!.conceptName,
                  vaccineDate!!
               )
            }
            dismissAllowingStateLoss()
         }
      }

      vaccineDateButton.setOnClickListener {
         AlreadyAdministeredVaccineDatePickerDialog(vaccineDate, this).show(childFragmentManager, "alreadyAdministeredVaccineDatePickerDialog")
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

   private fun validateDate() {
      if (withDate && vaccineDate == null) {
         val dateValidationText = getString(R.string.dialog_missing_substances_empty_date_validation_message)
         vaccineDateTextView.error = dateValidationText
         val hintTextColor = ContextCompat.getColor(requireContext(), R.color.errorLight)
         vaccineDateTextView.setHintTextColor(hintTextColor)
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

   override fun onDateSelected(dateTime: DateTime) {
      vaccineDate = dateTime
      vaccineDateTextView.text = dateTime.format(DateFormat.FORMAT_DATE)
      vaccineDateTextView.error = null
   }

   interface AddVaccineListener {
      fun addVaccine(vaccine: SubstanceDataModel)

      fun addVaccineDate(conceptName: String, dateValue: DateTime)
   }
}