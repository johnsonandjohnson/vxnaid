package com.jnj.vaccinetracker.common.data.network.apiexceptioninterceptor

import com.jnj.vaccinetracker.common.data.models.api.response.ApiErrorResponse
import com.jnj.vaccinetracker.common.exceptions.*
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException

/**
 * @author maartenvangiel
 * @version 1
 */
abstract class ApiExceptionInterceptorBase constructor(
    moshi: Moshi,
) : Interceptor {

    private val apiErrorResponseAdapter = moshi.adapter(ApiErrorResponse::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response = chain.proceed(request)
        return if (response.isSuccessful) {
            response
        } else {
            handleUnsuccessfulResponse(response, request)
        }
    }

    @Throws(IOException::class)
    private fun handleUnsuccessfulResponse(response: Response, request: Request): Response {

        val responseBody = response.body ?: return response
        val responseBodyString = responseBody.string()

        // When reading the response body it is consumed, so we need to construct a new response+responsebody so we can access it at a later time.
        val newResponseBody = responseBodyString.toResponseBody(responseBody.contentType())

        // Check for forbidden
        val responseCode = response.code
        if ((responseCode == 403 && responseBodyString.contains("Session timed out")) ||
            (responseCode == 401 &&
                    (responseBodyString.contains("User is not logged in") || responseBodyString.contains("Not authenticated")))
        ) {
            onSessionExpired()
        }
        if (responseCode == 404) {
            logError("API endpoint does not exist")
        }

        if (responseCode == 409) {
            throw DuplicateRequestException()
        }

        if (responseCode == 503) {
            throw ServerUnavailableException()
        }

        val apiErrorResponse = try {
            apiErrorResponseAdapter.fromJson(responseBodyString)
        } catch (e: Exception) {
            val url = request.url
            logError("Failed to parse API error response from JSON to API error model [url: ${url}]", e)
            null
        }
        apiErrorResponse?.let(::handleApiErrorResponse)

        return response.newBuilder().body(newResponseBody).build()
    }

    protected abstract fun onSessionExpired()

    private fun handleApiErrorResponse(apiErrorResponse: ApiErrorResponse) {
        logWarn("handleApiErrorResponse: ${apiErrorResponse.status}")
        // Crude error response handling for participation registration, backend returns a strange response out of spec here
        if (apiErrorResponse.message.contains("participant id already in use", ignoreCase = true)) {
            throw ParticipantAlreadyExistsException()
        }
        if (apiErrorResponse.message.contains("participant already exists with the same uuid", ignoreCase = true)) {
            throw ParticipantUuidAlreadyExistsException()
        }

        if (apiErrorResponse.message.contains("invalid template", ignoreCase = true)) {
            throw TemplateInvalidException()
        }

        if (apiErrorResponse.message.contains("template already exists for this participant", ignoreCase = true)) {
            throw TemplateAlreadyExistsException()
        }

        if (apiErrorResponse.message.contains("participant not found", ignoreCase = true)) {
            throw ParticipantNotFoundException()
        }

        when (apiErrorResponse.status) {
            ApiErrorResponse.STATUS_INVALID_SESSION,
            ApiErrorResponse.STATUS_SESSION_EXPIRED,
            -> {
                onSessionExpired()
            }
            ApiErrorResponse.STATUS_MATCH_NOT_FOUND -> {
                throw MatchNotFoundException()
            }
        }
    }
}