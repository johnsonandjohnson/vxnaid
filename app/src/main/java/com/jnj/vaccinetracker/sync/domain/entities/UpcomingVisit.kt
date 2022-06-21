package com.jnj.vaccinetracker.sync.domain.entities

import android.content.Context
import android.os.Parcelable
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.ui.minus
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.sync.data.models.AbsoluteScheduledVisit
import com.jnj.vaccinetracker.sync.data.models.ScheduledVisit
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class UpcomingVisit(val visitType: Type, val startDate: Date, val endDate: Date) : Parcelable {
    companion object {
        private val dateFormatDisplay = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
        fun ScheduledVisit.toUpcomingVisit(lastDosingVisit: VisitDetail): UpcomingVisit {
            val visitDate = lastDosingVisit.visitDate + daysFromLastDose.days
            val startDate = visitDate - windowBefore.days
            val endDate = visitDate + windowAfter.days
            return UpcomingVisit(Type.fromVisitType(nameOfDose), startDate, endDate)
        }

        fun AbsoluteScheduledVisit.toUpcomingVisit(): UpcomingVisit {
            return UpcomingVisit(Type.fromVisitType(scheduledVisit.nameOfDose), startDate, endDate)
        }

        fun VisitDetail.toUpcomingVisit() = UpcomingVisit(Type.fromVisitType(visitType), startDate, endDate)
    }

    val timeWindow: String
        get() {
            return if (startDate == endDate) {
                dateFormatDisplay.format(startDate)
            } else {
                "${dateFormatDisplay.format(startDate)} - ${dateFormatDisplay.format(endDate)}"
            }
        }

    enum class Type(val visitType: String) {
        DOSING(Constants.VISIT_TYPE_DOSING), IN_PERSON_FOLLOW_UP(Constants.VISIT_TYPE_OTHER);

        companion object {
            fun fromVisitType(visitType: String): Type {
                return when (visitType) {
                    DOSING.visitType -> DOSING
                    else -> IN_PERSON_FOLLOW_UP
                }
            }
        }

        fun displayName(context: Context) = when (this) {
            DOSING -> R.string.upcoming_visit_type_dosing
            IN_PERSON_FOLLOW_UP -> R.string.upcoming_visit_type_follow_up
        }.let { context.getString(it) }
    }

}

