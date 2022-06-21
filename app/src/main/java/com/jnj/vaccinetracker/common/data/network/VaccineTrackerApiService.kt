package com.jnj.vaccinetracker.common.data.network

import com.jnj.vaccinetracker.common.data.models.api.response.LoginResponse
import retrofit2.http.GET
import retrofit2.http.Header


interface VaccineTrackerApiService {

    @GET("openmrs/ws/rest/v1/session")
    suspend fun login(@Header("Authorization") basicAuth: String): LoginResponse
}