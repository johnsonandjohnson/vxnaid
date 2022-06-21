package com.jnj.vaccinetracker.common.data.models.api.response

import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.squareup.moshi.JsonClass

/**
 * @author druelens
 * @version 2
 */

@JsonClass(generateAdapter = true)
data class LicenseResponse(
    val licenses: List<License>,
)

@JsonClass(generateAdapter = true)
data class License(
    val type: LicenseType,
    val value: String?,
)