package com.jnj.vaccinetracker.common.data.models.api.response

import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * @author druelens
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class ObservationDto(
    val name: String,
    val value: String,
    val datetime: Date,
)

fun List<ObservationDto>.toMap() = distinctBy { it.name }.map { it.name to ObservationValue(it.value, it.datetime) }.toMap()