package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.GetUpcomingVisitUseCase.VisitSchemaItem.Companion.toVisitSchemaItem
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetVaccineScheduleUseCase
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.dateDayStart
import com.jnj.vaccinetracker.common.ui.minus
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.sync.data.models.ScheduledVisit
import com.jnj.vaccinetracker.sync.data.models.VisitType
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import kotlinx.coroutines.yield
import java.util.*
import javax.inject.Inject

class GetUpcomingVisitUseCase @Inject constructor(
    private val getVaccineScheduleUseCase: GetVaccineScheduleUseCase,
    private val getParticipantVisitDetailsUseCase: GetParticipantVisitDetailsUseCase,
    private val getParticipantRegimenUseCase: GetParticipantRegimenUseCase,
) {

    /**
     * for participant with [participantUuid ], get the first scheduled visit that comes after [date]
     * @param participantUuid for which participant
     * @param date since which date
     * @return null if no visit has occurred yet
     */
    suspend fun getUpcomingVisit(participantUuid: String, date: DateEntity): UpcomingVisit? {
        try {
            logInfo("getUpcomingVisit $participantUuid")
            val schedule = getVaccineScheduleUseCase.getMasterData()
            val regimen = getParticipantRegimenUseCase.getParticipantRegimen(participantUuid)
            val scheduleVisits = schedule.find { it.name == regimen }?.visits?.filter { it.visitType in Constants.SUPPORTED_UPCOMING_VISIT_TYPES }.orEmpty()
            val participantVisits = getParticipantVisitDetailsUseCase.getParticipantVisitDetails(participantUuid)
            val occurredDosingVisits = participantVisits
                .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
                .filter { it.isDosing() }
            val scheduledDosingVisits = participantVisits
                .filter { it.isScheduled() }
                .filter { it.isDosing() }
            val visitSchema = createVisitSchema(
                occurredDosingVisits = occurredDosingVisits,
                scheduledDosingVisits = scheduledDosingVisits,
                scheduledVisits = scheduleVisits
            )
            visitSchema.visits.forEachIndexed { index, visit ->
                logInfo("visit schema #$index: {}", visit)
            }

            return visitSchema.findNotOccurredVisitAfter(date)?.toUpcomingVisit().also {
                logInfo("found upcoming visit: {}", it)
            }
        } catch (ex: DosingNumberNotAvailableException) {
            logError("getUpcomingVisit error", ex)
            return null
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("getUpcomingVisit unknown error", ex)
            return null
        }
    }


    private class DosingNumberNotAvailableException(override val message: String) : Exception()

    private fun createVisitSchema(occurredDosingVisits: List<VisitDetail>, scheduledDosingVisits: List<VisitDetail>, scheduledVisits: List<ScheduledVisit>): VisitSchema {
        val schemaVisits = mutableListOf<VisitSchemaItem>()
        if (occurredDosingVisits.isNotEmpty()) {
            occurredDosingVisits.forEach { visit ->
                schemaVisits += visit.toVisitSchemaItem()
            }
            val firstOccurredDosingVisit = schemaVisits.find { it.doseNumber == 1 } ?: schemaVisits.first()
            var currentDosingVisit: VisitSchemaItem? = null
            scheduledVisits.forEach { scheduledVisit ->
                val current = currentDosingVisit
                if (current == null) {
                    if (scheduledVisit.isDosing() && firstOccurredDosingVisit.doseNumber == scheduledVisit.doseNumber) {
                        currentDosingVisit = firstOccurredDosingVisit
                    } else {
                        logWarn("skipped scheduled visit: {}", scheduledVisit)
                    }
                } else if (scheduledVisit.isDosing()) {
                    val newSchemaVisit = scheduledVisit.toVisitSchemaItem(current)
                    if (schemaVisits.none { it.doseNumber == scheduledVisit.doseNumber }) {
                        schemaVisits += newSchemaVisit
                    }
                    currentDosingVisit = newSchemaVisit
                } else {
                    schemaVisits += scheduledVisit.toVisitSchemaItem(current)
                }
            }
        } else {
            schemaVisits += listOfNotNull(scheduledDosingVisits.firstOrNull()?.toVisitSchemaItem())
        }
        val sortedVisits = schemaVisits.sortedBy { it.visitDate }
        return VisitSchema(sortedVisits)
    }

    private class VisitSchema(val visits: List<VisitSchemaItem>) {
        fun findNotOccurredVisitAfter(date: Date): VisitSchemaItem? {
            return visits.find { !it.hasOccurred && it.visitDate > date }
        }
    }

    private data class VisitSchemaItem(
        val type: VisitType,
        val doseNumber: Int,
        val visitDate: DateEntity,
        val startDate: DateEntity,
        val endDate: DateEntity,
        val hasOccurred: Boolean
    ) {
        fun toUpcomingVisit() = UpcomingVisit(visitType = UpcomingVisit.Type.fromVisitType(type.key), startDate = startDate, endDate = endDate)

        companion object {
            fun VisitDetail.toVisitSchemaItem(): VisitSchemaItem {
                val type = when (visitType) {
                    Constants.VISIT_TYPE_DOSING -> VisitType.DOSING
                    else -> VisitType.OTHER
                }
                val doseNumber = when (type) {
                    VisitType.DOSING -> dosingNumberOrThrow()
                    else -> dosingNumber ?: 0
                }
                return VisitSchemaItem(type, doseNumber, visitDate.dateDayStart, startDate.dateDayStart, endDate.dateDayStart, hasOccurred = isOccurred())
            }

            fun ScheduledVisit.toVisitSchemaItem(lastDose: VisitSchemaItem): VisitSchemaItem {
                val visitDate = lastDose.visitDate + daysFromLastDose.days
                val startDate = visitDate - windowBefore.days
                val endDate = visitDate + windowAfter.days
                return VisitSchemaItem(VisitType.fromKey(nameOfDose)!!, doseNumber, visitDate, startDate, endDate, hasOccurred = false)
            }
        }
    }

    companion object {
        private fun VisitDetail.dosingNumberOrThrow(): Int {
            return dosingNumber ?: throw DosingNumberNotAvailableException("visit ($this) has no dosing number")
        }

        private fun VisitDetail.isDosing() = visitType == Constants.VISIT_TYPE_DOSING
        private fun ScheduledVisit.isDosing() = visitType == VisitType.DOSING
        private fun VisitDetail.isScheduled() = visitStatus == Constants.VISIT_STATUS_SCHEDULED
        private fun VisitDetail.isOccurred() = visitStatus == Constants.VISIT_STATUS_OCCURRED
    }
}