package com.jnj.vaccinetracker.common.data.models.api.response

import com.squareup.moshi.JsonClass

/**
 * @author maartenvangiel
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val message: String,
    val status: String?,
    val details: String?
) {

    companion object {
        const val STATUS_INVALID_SESSION = "INVALID_SESSION"
        const val STATUS_SESSION_EXPIRED = "com.janssen.solidaritytrail.exception.CustomAPIException: Session expired"
        const val STATUS_MATCH_NOT_FOUND = "MATCH_NOT_FOUND"
    }

}