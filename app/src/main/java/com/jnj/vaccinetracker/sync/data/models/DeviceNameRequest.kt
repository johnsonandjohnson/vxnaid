package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceNameRequest(val siteUuid: String)