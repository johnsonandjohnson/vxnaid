package com.jnj.vaccinetracker.common.data.models.api.response

import com.squareup.moshi.JsonClass

/**
 * @author maartenvangiel
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class AttributeDto(
    val type: String,
    val value: String,
)

