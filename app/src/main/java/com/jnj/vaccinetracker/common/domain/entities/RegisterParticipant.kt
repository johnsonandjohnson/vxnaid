package com.jnj.vaccinetracker.common.domain.entities

import java.util.*

data class RegisterParticipant(
    val participantId: String,
    val nin: String?,
    val gender: Gender,
    val birthDate: BirthDate,
    val address: Address,
    val attributes: Map<String, String>,
    val image: ImageBytes?,
    val biometricsTemplate: BiometricsTemplateBytes?,
    val scheduleFirstVisit: ScheduleFirstVisit,
) {
    val birthWeight: String? = null
}

data class ScheduleFirstVisit(
    val visitType: String,
    val startDatetime: Date,
    val locationUuid: String,
    val attributes: Map<String, String>,
)