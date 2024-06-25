package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeSpan
import java.util.Calendar

class BirthDatePickerDialog(
    private var selectedDate: DateTime? = null,
    private var isBirthDateEstimatedChecked: Boolean = false,
    private var yearsEstimated: Int? = null,
    private var monthsEstimated: Int? = null,
    private var daysEstimated: Int? = null
) : DialogFragment() {

    private lateinit var estimatedBirthdayLayout: LinearLayout
    private lateinit var switchIsBirthDateEstimated: SwitchCompat
    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    private lateinit var datePicker: DatePicker
    private lateinit var numberPickerYears: NumberPicker
    private lateinit var numberPickerMonths: NumberPicker
    private lateinit var numberPickerDays: NumberPicker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_birth_date_picker)

        initializeViews(dialog)
        setupDatePicker()
        setupIsBirthDateEstimatedSwitch()
        setupNumberPickers()
        setupSwitchListener()

        btnOk.setOnClickListener {
            selectedDate = if (switchIsBirthDateEstimated.isChecked) {
                calculateEstimatedDate()
            } else {
                DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
            }
            isBirthDateEstimatedChecked = switchIsBirthDateEstimated.isChecked

            val years = if (switchIsBirthDateEstimated.isChecked) numberPickerYears.value else null
            val months = if (switchIsBirthDateEstimated.isChecked) numberPickerMonths.value else null
            val days = if (switchIsBirthDateEstimated.isChecked) numberPickerDays.value else null

            (parentFragment as? BirthDatePickerListener)?.onBirthDatePicked(
                selectedDate!!,
                isBirthDateEstimatedChecked,
                years,
                months,
                days
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
        estimatedBirthdayLayout = dialog.findViewById(R.id.layout_estimated_birthday)
        switchIsBirthDateEstimated = dialog.findViewById(R.id.switch_is_birth_date_estimated)
        btnOk = dialog.findViewById(R.id.btn_ok)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
        datePicker = dialog.findViewById(R.id.datePicker)
        numberPickerYears = dialog.findViewById(R.id.numberPicker_years)
        numberPickerMonths = dialog.findViewById(R.id.numberPicker_months)
        numberPickerDays = dialog.findViewById(R.id.numberPicker_days)
    }

    private fun setupDatePicker() {
        selectedDate?.let {
            datePicker.updateDate(it.yearInt, it.month1, it.dayOfMonth)
        }

        val c = Calendar.getInstance()
        datePicker.maxDate = c.timeInMillis

        if (selectedDate == null) {
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH) + 1
            val day = c.get(Calendar.DAY_OF_MONTH)
            datePicker.init(year, month, day, null)
        }
    }

    private fun setupIsBirthDateEstimatedSwitch() {
        switchIsBirthDateEstimated.isChecked = isBirthDateEstimatedChecked
        updateLayoutVisibility()
    }

    private fun setupNumberPickers() {
        val yearLabels = Array(101) { index -> "$index Years" }
        val monthLabels = Array(12) { index -> "$index Months" }
        val dayLabels = Array(31) { index -> "$index Days" }

        numberPickerYears.minValue = 0
        numberPickerYears.maxValue = yearLabels.size - 1
        numberPickerYears.displayedValues = yearLabels
        numberPickerYears.value = yearsEstimated ?: 0

        numberPickerMonths.minValue = 0
        numberPickerMonths.maxValue = monthLabels.size - 1
        numberPickerMonths.displayedValues = monthLabels
        numberPickerMonths.value = monthsEstimated ?: 0

        numberPickerDays.minValue = 0
        numberPickerDays.maxValue = dayLabels.size - 1
        numberPickerDays.displayedValues = dayLabels
        numberPickerDays.value = daysEstimated ?: 0

        numberPickerYears.wrapSelectorWheel = false
        numberPickerMonths.wrapSelectorWheel = false
        numberPickerDays.wrapSelectorWheel = false
    }

    private fun setupSwitchListener() {
        switchIsBirthDateEstimated.setOnCheckedChangeListener { _, _ ->
            updateLayoutVisibility()
        }
    }

    private fun updateLayoutVisibility() {
        if (switchIsBirthDateEstimated.isChecked) {
            datePicker.visibility = LinearLayout.GONE
            estimatedBirthdayLayout.visibility = LinearLayout.VISIBLE
        } else {
            datePicker.visibility = LinearLayout.VISIBLE
            estimatedBirthdayLayout.visibility = LinearLayout.GONE
        }
    }

    private fun calculateEstimatedDate(): DateTime {
        val years = numberPickerYears.value
        val months = numberPickerMonths.value
        val days = numberPickerDays.value

        val dateTimeSpan = DateTimeSpan(years = years, months = months, days = days)

        return DateTime.now().minus(dateTimeSpan)
    }

    interface BirthDatePickerListener {
        fun onBirthDatePicked(birthDate: DateTime?,
                              isChecked: Boolean,
                              yearsEstimated: Int?,
                              monthsEstimated: Int?,
                              daysEstimated: Int?)
    }
}
