package com.jnj.vaccinetracker.common.data.network

import com.jnj.vaccinetracker.common.data.helpers.WebCallUtil
import com.jnj.vaccinetracker.common.data.models.api.response.LoginResponse
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.di.qualifiers.MainApi
import okhttp3.Credentials
import javax.inject.Inject

interface VaccineTrackerApiDataSource : VaccineTrackerApiDataSourceBase {
    suspend fun login(username: String, password: String): LoginResponse
}

/**
 * A wrapper class of the [VaccineTrackerApiService] which abstracts away MultiPartBody
 * and wraps each call with [webCallUtil]
 */
open class VaccineTrackerApiDataSourceDefault @Inject constructor(
    private val webCallUtil: WebCallUtil,
    private val apiService: VaccineTrackerApiService,
    @MainApi
    private val cookieRepository: CookieRepository,
) : VaccineTrackerApiDataSource {

    override suspend fun login(username: String, password: String): LoginResponse = webCallUtil.webCall(false, callName = "login") {
        cookieRepository.clearSessionCookie()
        apiService.login(Credentials.basic(username, password))
    }
}