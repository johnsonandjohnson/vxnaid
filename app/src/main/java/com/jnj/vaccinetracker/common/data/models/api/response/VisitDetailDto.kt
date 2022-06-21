package com.jnj.vaccinetracker.common.data.models.api.response

import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.sync.data.models.toMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class VisitDetailDto(
    @Json(name = "visitUuid") val uuid: String,
    val visitType: String,
    @Json(name = "startDatetime") val visitDate: Date,
    val attributes: List<AttributeDto>,
    val observations: List<ObservationDto>,
)

fun VisitDetailDto.toDomain() =
    VisitDetail(uuid = uuid,
        visitType = visitType,
        visitDate = visitDate,
        attributes = attributes.toMap(),
        observations = observations.toMap()
    )

