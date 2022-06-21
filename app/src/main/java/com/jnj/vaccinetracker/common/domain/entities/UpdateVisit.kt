package com.jnj.vaccinetracker.common.domain.entities

import java.util.*

data class UpdateVisit(
    val visitUuid: String,
    val participantUuid: String,
    val startDatetime: Date,
    val locationUuid: String,
    val attributes: Map<String, String>,
    val observations: Map<String, String>,
)