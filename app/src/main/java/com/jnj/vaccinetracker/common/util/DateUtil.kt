package com.jnj.vaccinetracker.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateUtil {
    companion object {

        fun convertDateToString(date: Date, targetFormat: String): String {
            val format = SimpleDateFormat(targetFormat, Locale.getDefault())
            return format.format(date)
        }

        fun convertStringToDate(date: String, sourceFormat: String): Date? {
            val dateFormat = SimpleDateFormat(sourceFormat, Locale.getDefault())
            return dateFormat.parse(date)
        }
    }
}