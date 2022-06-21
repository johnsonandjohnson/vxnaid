package com.jnj.vaccinetracker.fake.data.random

import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.data.models.api.response.ObservationDto
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.sync.data.models.ParticipantSyncRecord
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.models.VisitSyncRecord
import javax.inject.Inject

class RandomVisitsGenerator @Inject constructor() {

    private fun generateVisit(participantSyncRecord: ParticipantSyncRecord.Update, startDateTime: SyncDate, dose: Int, missedVisit: Boolean): VisitSyncRecord {

        fun createAttributes(): List<AttributeDto> {
            return listOf(
                Constants.ATTRIBUTE_VISIT_STATUS to if (missedVisit) Constants.VISIT_STATUS_MISSED else Constants.VISIT_STATUS_OCCURRED,
                Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to dose.toString(),
                Constants.ATTRIBUTE_OPERATOR to participantSyncRecord.attributes.find { it.type == Constants.ATTRIBUTE_OPERATOR }!!.value,
            ).map { AttributeDto(it.first, it.second) }
        }

        fun createObservations(observationDate: SyncDate): List<ObservationDto> {
            if (missedVisit) return emptyList()
            return listOf(
                Constants.OBSERVATION_TYPE_BARCODE to "1325425962535",
                Constants.OBSERVATION_TYPE_MANUFACTURER to "Pfizer"
            ).map { ObservationDto(it.first, it.second, observationDate.date) }
        }

        @Suppress("UnnecessaryVariable")
        val observationDate = startDateTime
        return VisitSyncRecord.Update(participantUuid = participantSyncRecord.participantUuid,
            dateModified = startDateTime, visitUuid = uuid(), visitType = Constants.VISIT_TYPE_DOSING,
            startDateTime.date, attributes = createAttributes(), observations = createObservations(observationDate)
        )
    }

    fun generateVisits(participantSyncRecord: ParticipantSyncRecord.Update): List<VisitSyncRecord> {
        val startDateTime = participantSyncRecord.dateModified

        return listOf(generateVisit(participantSyncRecord, startDateTime, 1, false), generateVisit(participantSyncRecord, startDateTime + 1.days, 2, true))
    }
}