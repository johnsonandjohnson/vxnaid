package com.jnj.vaccinetracker.common.data.models.api.request

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateVisitObservationDto(
    val name: String,
    val value: String,
)