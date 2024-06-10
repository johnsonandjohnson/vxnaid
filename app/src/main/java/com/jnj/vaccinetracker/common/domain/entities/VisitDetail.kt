package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.models.Constants
import java.text.SimpleDateFormat
import java.util.*

data class VisitDetail(
    val uuid: String,
    val visitType: String,
    val visitDate: Date,
    val attributes: Map<String, String>,
    val observations: Map<String, ObservationValue>,
) {

    val dosingNumber: Int? get() = attributes[Constants.ATTRIBUTE_VISIT_DOSE_NUMBER]?.toIntOrNull()
    val weight: Int? get() = attributes[Constants.ATTRIBUTE_VISIT_WEIGHT]?.toIntOrNull()
    val height: Int? get() = attributes[Constants.ATTRIBUTE_VISIT_HEIGHT]?.toIntOrNull()
    val manufacturer: String? get() = observations[Constants.OBSERVATION_TYPE_MANUFACTURER]?.value

    val encounterDate: Date? get() = observations[Constants.OBSERVATION_TYPE_MANUFACTURER]?.dateTime
    val encounterTimeDisplay: String? get() = encounterDate?.let { dateFormatDisplay.format(it) }
    val timeWindow: String
        get() {
            return if (startDate == endDate) {
                dateFormatDisplay.format(startDate)
            } else {
                "${dateFormatDisplay.format(startDate)} - ${dateFormatDisplay.format(endDate)}"
            }
        }

    val startDate: Date
        get() {
            val daysBefore = attributes[Constants.ATTRIBUTE_VISIT_DAYS_BEFORE]?.toIntOrNull()
            val cal = Calendar.getInstance().apply { time = visitDate }
            daysBefore?.let { cal.add(Calendar.DAY_OF_YEAR, -daysBefore) }
            return cal.time
        }

    val endDate: Date
        get() {
            val daysAfter = attributes[Constants.ATTRIBUTE_VISIT_DAYS_AFTER]?.toIntOrNull()
            val cal = Calendar.getInstance().apply { time = visitDate }
            daysAfter?.let { cal.add(Calendar.DAY_OF_YEAR, daysAfter) }
            return cal.time
        }

    val visitStatus: String? get() = attributes[Constants.ATTRIBUTE_VISIT_STATUS]

    companion object {
        private val dateFormatDisplay = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
    }
}
