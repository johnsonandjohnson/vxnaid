package com.jnj.vaccinetracker.common.data.models.api.request

import com.squareup.moshi.JsonClass
import java.util.*

/**
 * @author maartenvangiel
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class VisitCreateRequest(
    val participantUuid: String,
    val visitUuid: String,
    val visitType: String,
    val startDatetime: Date,
    val locationUuid: String,
    val attributes: List<CreateVisitAttributeDto>,
)

