package com.jnj.vaccinetracker.common.data.models.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @author maartenvangiel
 * @version 1
 */
@JsonClass(generateAdapter = true)
class RegisterParticipantResponse(
    @Json(name = "uuid") val patientUuid: String,
    val isIrisRegistered: Boolean
)