package com.jnj.vaccinetracker.visit.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogScheduleMissingSubstancesBinding
import com.jnj.vaccinetracker.visit.adapters.MissingSubstanceItemAdapter
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import java.util.Date

class DialogScheduleMissingSubstances(
   private val missingSubstanceLabels: List<String>
) : BaseDialogFragment(), DatePickerDialog.DatePickerListener {

   private var datePicked: Date? = null
   var dateText: String? = null

   companion object {
      const val TAG_DATE_PICKER = "datePicker"
   }

   private lateinit var binding: DialogScheduleMissingSubstancesBinding

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setStyle(STYLE_NO_TITLE, 0)
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_schedule_missing_substances, container, false)
      binding.executePendingBindings()

      binding.btnConfirm.setOnClickListener {
         validateDate()
         if (datePicked != null) {
            findParent<DialogScheduleMissingSubstancesListener>()?.onDialogScheduleMissingSubstancesConfirmed(datePicked!!)
            dismissAllowingStateLoss()
         }
      }

      binding.btnCancel.setOnClickListener {
         dismissAllowingStateLoss()
      }

      binding.btnPickDate.setOnClickListener {
         DatePickerDialog().show(parentFragmentManager, TAG_DATE_PICKER)
      }

      setupRecyclerView()
      return binding.root
   }

   private fun setupRecyclerView() {
      val adapter = MissingSubstanceItemAdapter(missingSubstanceLabels)
      binding.recyclerViewMissingSubstances.layoutManager = LinearLayoutManager(requireContext())
      binding.recyclerViewMissingSubstances.adapter = adapter
   }

   interface DialogScheduleMissingSubstancesListener {
      fun onDialogScheduleMissingSubstancesConfirmed(date: Date)
   }

   private fun validateDate() {
      if (datePicked == null) {
         val dateValidationText = getString(R.string.dialog_missing_substances_empty_date_validation_message)
         binding.textViewDate.error = dateValidationText

         val hintTextColor = ContextCompat.getColor(requireContext(), R.color.errorLight)
         binding.textViewDate.setHintTextColor(hintTextColor)
      }
   }

   override fun onDatePicked(date: DateTime?) {
      datePicked = date?.let { Date(it.unixMillisLong) }
      val dateFormat = DateFormat("yyyy-MM-dd")
      dateText = date?.format(dateFormat) ?: ""
      binding.textViewDate.text = dateText
      binding.textViewDate.error = null

      val hintTextColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
      binding.textViewDate.setHintTextColor(hintTextColor)
   }
}
