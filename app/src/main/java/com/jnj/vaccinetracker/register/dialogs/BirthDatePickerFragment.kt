package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.soywiz.klock.DateTime
import java.util.Calendar


class BirthDatePickerDialog(
        private var selectedDate: DateTime? = null,
        private var isBirthDateEstimatedChecked: Boolean = false)
    : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_birth_date_picker)

        val datePicker = dialog.findViewById<DatePicker>(R.id.datePicker)
        val checkBox = dialog.findViewById<CheckBox>(R.id.checkbox_is_birth_date_estimated)

        selectedDate?.let {
            datePicker.updateDate(it.yearInt, it.month1, it.dayOfMonth)
        }
        checkBox.isChecked = isBirthDateEstimatedChecked

        if (selectedDate == null) {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            datePicker.init(year, month, day, null)
        }

        dialog.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            selectedDate = DateTime(datePicker.year, datePicker.month, datePicker.dayOfMonth)
            isBirthDateEstimatedChecked = checkBox.isChecked
            findParent<BirthDatePickerListener>()?.onBirthDatePicked(selectedDate!!, isBirthDateEstimatedChecked)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    interface BirthDatePickerListener {
        fun onBirthDatePicked(birthDate: DateTime, isChecked: Boolean)
    }
}

