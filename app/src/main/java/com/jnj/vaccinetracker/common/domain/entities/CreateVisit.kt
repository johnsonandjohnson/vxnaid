package com.jnj.vaccinetracker.common.domain.entities

import java.util.*

data class CreateVisit(
    val participantUuid: String,
    val visitType: String,
    val startDatetime: Date,
    val locationUuid: String,
    val attributes: Map<String, String>,
)