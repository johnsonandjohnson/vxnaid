package com.jnj.vaccinetracker.visit.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.soywiz.klock.DateTime
import java.util.Calendar

class DatePickerDialog(
    private var selectedDate: DateTime? = null,
) : DialogFragment() {

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button
    private lateinit var datePicker: DatePicker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_date_picker)

        initializeViews(dialog)
        setupDatePicker()

        btnOk.setOnClickListener {
            selectedDate = DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
            findParent<DatePickerListener>()?.onDatePicked(
                selectedDate!!,
            )
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun initializeViews(dialog: Dialog) {
        datePicker = dialog.findViewById(R.id.datePicker)
        btnOk = dialog.findViewById(R.id.btn_ok)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
    }

    private fun setupDatePicker() {
        selectedDate?.let {
            datePicker.updateDate(it.yearInt, it.month1 - 1, it.dayOfMonth)
        } ?: run {
            val c = Calendar.getInstance()
            datePicker.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null)
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        val minDateInMillis = calendar.timeInMillis
        datePicker.minDate = minDateInMillis
    }

    interface DatePickerListener {
        fun onDatePicked(date: DateTime?)
    }
}
