package com.jnj.vaccinetracker.common.domain.entities

import com.squareup.moshi.JsonClass

/**
 * @author druelens
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class VaccineTrackerVersion(
    val version: Int,
    val url: String,
)
