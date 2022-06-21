package com.jnj.vaccinetracker.common.data.models.api.request

import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReleaseLicensesRequest(val licenseTypes: List<LicenseType>)