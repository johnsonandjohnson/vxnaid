package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.ui.minus
import com.jnj.vaccinetracker.common.ui.plus
import com.squareup.moshi.*
import java.util.*

typealias VaccineSchedule = List<VaccineRegimen>

fun Moshi.vaccineScheduleAdapter(): JsonAdapter<VaccineSchedule> = adapter(Types.newParameterizedType(List::class.java, VaccineRegimen::class.java))

@JsonClass(generateAdapter = true)
data class VaccineRegimen(
    val name: String,
    @Json(name = "numberOfDose")
    val doseCount: Int,
    val visits: List<ScheduledVisit>,
)


@JsonClass(generateAdapter = true)
data class ScheduledVisit(
    val doseNumber: Int,
    @Json(name = "nameOfDose")
    val nameOfDose: String,
    @Json(name = "numberOfFutureVisit")
    val futureVisitCount: Int = 0,
    @Json(name = "lowWindow")
    val windowBefore: Int,
    @Json(name = "midPointWindow")
    val daysFromLastDose: Int,
    @Json(name = "upWindow")
    val windowAfter: Int,
) {
    val visitType: VisitType? get() = VisitType.fromKey(nameOfDose)
}

data class AbsoluteScheduledVisit(val scheduledVisit: ScheduledVisit, val startDate: Date, val endDate: Date, val visitDate: Date)

fun List<ScheduledVisit>.withAbsoluteDates(firstDosingVisitDate: Date): List<AbsoluteScheduledVisit> {
    var lastDosingVisit = firstDosingVisitDate

    return map { scheduledVisit ->
        val date = lastDosingVisit + scheduledVisit.daysFromLastDose.days
        val minDate = date - scheduledVisit.windowBefore.days
        val maxDate = date + scheduledVisit.windowAfter.days
        if (scheduledVisit.visitType == VisitType.DOSING) {
            lastDosingVisit = date
        }
        AbsoluteScheduledVisit(scheduledVisit = scheduledVisit, startDate = minDate, endDate = maxDate, visitDate = date)
    }
}