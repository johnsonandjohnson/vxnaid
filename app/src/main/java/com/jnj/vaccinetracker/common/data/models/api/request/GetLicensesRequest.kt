package com.jnj.vaccinetracker.common.data.models.api.request

import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetLicensesRequest(val licenseTypes: List<LicenseType>)