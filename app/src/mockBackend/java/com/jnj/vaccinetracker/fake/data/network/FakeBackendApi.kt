package com.jnj.vaccinetracker.fake.data.network

import com.jnj.vaccinetracker.common.data.models.ParticipantMatchDto
import com.jnj.vaccinetracker.common.data.models.api.request.RegisterParticipantRequest
import com.jnj.vaccinetracker.common.data.models.api.request.VisitCreateRequest
import com.jnj.vaccinetracker.common.data.models.api.request.VisitUpdateRequest
import com.jnj.vaccinetracker.common.data.models.api.response.ConfigurationDto
import com.jnj.vaccinetracker.common.data.models.api.response.LoginResponse
import com.jnj.vaccinetracker.common.data.models.api.response.RegisterParticipantResponse
import com.jnj.vaccinetracker.common.data.models.api.response.VisitDetailDto
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.exceptions.InvalidSessionException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.data.models.*
import kotlinx.coroutines.yield
import okhttp3.ResponseBody
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FakeBackendApi @Inject constructor(private val fakeBackendDatabase: FakeBackendDatabase, private val mockAssetReader: MockAssetReader) {

    private val userIdMap = mapOf("admin" to "1c3db49d-440a-11e6-a65c-00e04c680037", "syncadmin" to "ff422103-cf53-44f0-bee6-21cb3f8b25be")

    private suspend fun beforeWebCall() {
        /*
        Fake backend so no use in using/calling beforeWebCall.
         */
    }

    private suspend fun <T> executeWebCall(callName: String = "", block: suspend () -> T): T {
        try {
            logInfo("executeWebCall FAKE: $callName")
            return block()
        } catch (ex: InvalidSessionException) {
            throw ex
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            throw WebCallException("Something went wrong during fake webCall '$callName'", ex, null, null)
        }
    }

    private suspend fun <T> webCall(requireAuthentication: Boolean = true, block: suspend () -> T): T {
        beforeWebCall()
        return executeWebCall(callName = "", block)
    }

    private suspend fun <T> webCallSync(requireAuthentication: Boolean = true, block: suspend () -> T): T {
        beforeWebCall()
        return executeWebCall(callName = "", block)
    }

    suspend fun getParticipantVisitDetails(participantUuid: String): List<VisitDetailDto> = webCall {
        emptyList()
    }

    suspend fun matchParticipants(participantId: String?, phone: String?, biometricsTemplateBytes: BiometricsTemplateBytes?): List<ParticipantMatchDto> = webCall {
        emptyList()
    }

    suspend fun getPersonImage(participantUuid: String): ResponseBody? = webCall {
        null
    }

    suspend fun login(username: String): LoginResponse {
        val lowerUsername = username.toLowerCase(Locale.ROOT)
        val userId = userIdMap[lowerUsername] ?: throw IOException("invalid credentials")
        return mockAssetReader.readLoginResponse().let {
            it.copy(user = it.user?.copy(uuid = userId, display = username, username = username))
        }
    }

    suspend fun loginOperator(username: String, password: String): LoginResponse = webCall {
        login(username)
    }

    suspend fun loginSyncAdmin(username: String, password: String): LoginResponse = webCall {
        login(username)
    }

    suspend fun reportSyncComplete(reportSyncCompleteRequest: SyncCompleteRequest) = webCall {
        Unit
    }

    suspend fun getCountryAddressHierarchy() = webCall {
        mockAssetReader.readAddressHierarchy()
    }

    suspend fun getActiveUsers() = webCall {
        mockAssetReader.readActiveUsers()
    }

    suspend fun getVaccineSchedule() = webCall {
        mockAssetReader.readVaccineSchedule()
    }

    suspend fun getSites() = webCall {
        mockAssetReader.readSites()
    }

    suspend fun getLocalization() = webCall {
        mockAssetReader.readLocalization()
    }

    private fun ConfigurationDto.setUp() = copy()
    suspend fun getConfiguration() = webCall {
        mockAssetReader.readConfiguration().setUp()
    }

    suspend fun getMasterDataUpdates(): MasterDataUpdatesResponse = webCall {
        emptyList()
    }

    suspend fun updateVisit(visitUpdateRequest: VisitUpdateRequest): Unit = webCall {
        logInfo("updateVisit fake")
    }

    suspend fun createVisit(visitCreateRequest: VisitCreateRequest): Unit = webCall {
        logInfo("createVisit fake")
    }

    suspend fun getAllParticipants(syncRequest: SyncRequest): ParticipantSyncResponse = webCall {
        fakeBackendDatabase.getAllParticipants(syncRequest)
    }

    suspend fun getAllParticipantBiometricsTemplates(syncRequest: SyncRequest): ParticipantBiometricsTemplateSyncResponse = webCall {
        fakeBackendDatabase.getAllParticipantBiometricsTemplates(syncRequest)
    }

    suspend fun getAllParticipantImages(syncRequest: SyncRequest): ParticipantImageSyncResponse = webCall {
        fakeBackendDatabase.getAllParticipantImages(syncRequest)
    }

    suspend fun getAllVisits(syncRequest: SyncRequest): VisitSyncResponse = webCall {
        fakeBackendDatabase.getAllVisits(syncRequest)
    }

    suspend fun registerParticipant(registerParticipantRequest: RegisterParticipantRequest, biometricsTemplate: BiometricsTemplateBytes?): RegisterParticipantResponse = webCall {
        logInfo("registerParticipant fake")
        RegisterParticipantResponse(registerParticipantRequest.participantUuid, biometricsTemplate != null)
    }

}