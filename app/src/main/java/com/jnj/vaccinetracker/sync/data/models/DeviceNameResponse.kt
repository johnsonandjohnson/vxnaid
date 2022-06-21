package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @author Timo Nelen
 * @version 1
 */
@JsonClass(generateAdapter = true)
class DeviceNameResponse(
    @Json(name = "deviceName") val deviceName: String,
)