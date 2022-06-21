package com.jnj.vaccinetracker.common.data.models.api.request

import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * @author druelens
 * @version 2
 */
@JsonClass(generateAdapter = true)
data class VisitUpdateRequest(
    val visitUuid: String,
    val startDatetime: Date,
    val locationUuid: String,
    val attributes: List<AttributeDto>,
    val observations: List<UpdateVisitObservationDto>,
)

