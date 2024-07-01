package com.jnj.vaccinetracker.sync.data.network

import com.jnj.vaccinetracker.common.data.helpers.WebCallUtil
import com.jnj.vaccinetracker.common.data.models.ParticipantMatchDto
import com.jnj.vaccinetracker.common.data.models.api.request.*
import com.jnj.vaccinetracker.common.data.models.api.response.*
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiDataSourceBase
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.di.qualifiers.SyncApi
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.sync.data.models.*
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface VaccineTrackerSyncApiDataSource : VaccineTrackerApiDataSourceBase {
    suspend fun login(syncUserCredentials: SyncUserCredentials): LoginResponse

    suspend fun getAllVisits(syncRequest: SyncRequest): VisitSyncResponse

    suspend fun getAllParticipants(syncRequest: SyncRequest): ParticipantSyncResponse

    suspend fun getAllParticipantBiometricsTemplates(syncRequest: SyncRequest): ParticipantBiometricsTemplateSyncResponse

    suspend fun getAllParticipantImages(syncRequest: SyncRequest): ParticipantImageSyncResponse

    suspend fun getMasterDataUpdates(): MasterDataUpdatesResponse

    suspend fun getConfiguration(): ConfigurationDto

    suspend fun getLocalization(): LocalizationMapDto

    suspend fun getSites(): SitesDto

    suspend fun getCountryAddressHierarchy(): AddressHierarchyDto

    suspend fun getActiveUsers(): ActiveUsersResponse

    suspend fun registerParticipant(
        registerParticipantRequest: RegisterParticipantRequest,
        biometricsTemplate: BiometricsTemplateBytes?,
    ): RegisterParticipantResponse

    suspend fun updateVisit(visitUpdateRequest: VisitUpdateRequest)

    suspend fun createVisit(visitCreateRequest: VisitCreateRequest)

    suspend fun getLicenses(getLicensesRequest: GetLicensesRequest): LicenseResponse

    suspend fun releaseLicenses(releaseLicensesRequest: ReleaseLicensesRequest)

    suspend fun reportSyncComplete(syncCompleteRequest: SyncCompleteRequest)

    suspend fun getParticipantsByUuids(getParticipantsByUuidsRequest: GetParticipantsByUuidsRequest): List<ParticipantSyncRecord>

    suspend fun getImagesByUuids(getImagesByUuidsRequest: GetImagesByUuidsRequest): List<ParticipantImageSyncRecord>

    suspend fun getVisitsByUuids(getVisitsByUuidsRequest: GetVisitsByUuidsRequest): List<VisitSyncRecord>

    suspend fun getBiometricsTemplatesByUuids(getBiometricsTemplatesByUuidsRequest: GetBiometricsTemplatesByUuidsRequest): List<ParticipantBiometricsTemplateSyncRecord>

    suspend fun uploadSyncErrors(syncErrorsRequest: SyncErrorsRequest)

    suspend fun markSyncErrorsResolved(markSyncErrorsResolved: MarkSyncErrorsResolvedRequest)

    suspend fun getVaccineSchedule(): VaccineSchedule

    suspend fun getSubstancesConfig(): SubstancesConfig

    suspend fun getDeviceName(deviceNameRequest: DeviceNameRequest): DeviceNameResponse

    suspend fun getPersonImage(personUuid: String): ResponseBody?

    suspend fun getParticipantVisitDetails(participantUuid: String): List<VisitDetailDto>

    suspend fun matchParticipants(participantId: String?, phone: String?, biometricsTemplateFile: BiometricsTemplateBytes?, country: String): List<ParticipantMatchDto>

    suspend fun personTemplate(participantUuid: String, biometricsTemplate: BiometricsTemplateBytes)

    suspend fun getLatestVersion(): VersionDto
}

@Singleton
@Named("default")
class VaccineTrackerSyncApiDataSourceDefault @Inject constructor(
    private val webCallUtil: WebCallUtil,
    private val apiService: VaccineTrackerSyncApiService,
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    @SyncApi
    private val syncCookieRepository: CookieRepository,
) : VaccineTrackerSyncApiDataSource {
    private companion object {
        private const val PART_ID_PARTICIPANT_ID = "participantId"
        private const val PART_ID_PHONE = "phone"
        private const val PART_ID_COUNTRY = "country"
        private const val PART_ID_IRIS_TEMPLATE = "template"

        fun getBiometricsTemplateBytesMultiPart(biometricsTemplate: BiometricsTemplateBytes): MultipartBody.Part {
            val requestBody: RequestBody = biometricsTemplate.bytes.toRequestBody("multipart/form-data".toMediaType())
            return MultipartBody.Part.createFormData(PART_ID_IRIS_TEMPLATE, "biometrics_template.dat", requestBody)
        }

        private fun getParticipantIdPart(participantId: String): MultipartBody.Part {
            return MultipartBody.Part.createFormData(PART_ID_PARTICIPANT_ID, participantId)
        }

        private fun getPhonePart(phone: String): MultipartBody.Part {
            return MultipartBody.Part.createFormData(PART_ID_PHONE, phone)
        }

        private fun getCountryPart(country: String): MultipartBody.Part {
            return MultipartBody.Part.createFormData(PART_ID_COUNTRY, country)
        }
    }

    private val mutex = Mutex()
    override suspend fun login(syncUserCredentials: SyncUserCredentials): LoginResponse = webCallSync(false, callName = "login") {
        apiService.login(syncUserCredentials.basicAuth())
    }

    override suspend fun getAllVisits(syncRequest: SyncRequest): VisitSyncResponse = webCallSync(callName = "getAllVisits") {
        apiService.getAllVisits(syncRequest)
    }

    override suspend fun getAllParticipants(syncRequest: SyncRequest): ParticipantSyncResponse = webCallSync(callName = "getAllParticipants") {
        apiService.getAllParticipants(syncRequest)
    }

    override suspend fun getAllParticipantBiometricsTemplates(syncRequest: SyncRequest): ParticipantBiometricsTemplateSyncResponse =
        webCallSync(callName = "getAllParticipantBiometricsTemplates") {
            apiService.getAllParticipantBiometricsTemplates(syncRequest)
        }

    override suspend fun getAllParticipantImages(syncRequest: SyncRequest): ParticipantImageSyncResponse = webCallSync(callName = "getAllParticipantImages") {
        apiService.getAllParticipantImages(syncRequest)
    }

    override suspend fun getMasterDataUpdates(): MasterDataUpdatesResponse = webCallSync(callName = "getMasterDataUpdates") {
        apiService.getMasterDataUpdates()
    }

    override suspend fun getConfiguration(): ConfigurationDto = webCallSync(callName = "getConfiguration") {
        apiService.getConfiguration()
    }

    override suspend fun getLocalization(): LocalizationMapDto = webCallSync(callName = "getLocalization") {
        apiService.getLocalization()
    }

    override suspend fun getSites() = webCallSync(callName = "getSites") {
        apiService.getSites()
    }

    override suspend fun getCountryAddressHierarchy(): AddressHierarchyDto = webCallSync(callName = "getCountryAddressHierarchy") {
        apiService.getCountryAddressHierarchy()
    }

    override suspend fun getActiveUsers(): ActiveUsersResponse = webCallSync(callName = "getActiveUsers") {
        apiService.getActiveUsers()
    }

    override suspend fun registerParticipant(
        registerParticipantRequest: RegisterParticipantRequest,
        biometricsTemplate: BiometricsTemplateBytes?,
    ): RegisterParticipantResponse {
        val biometricsTemplateFormData = biometricsTemplate?.let { getBiometricsTemplateBytesMultiPart(it) }
        return webCallSync(callName = "registerParticipant") {
            apiService.registerParticipant(registerParticipantRequest, biometricsTemplateFormData)
        }
    }

    override suspend fun updateVisit(visitUpdateRequest: VisitUpdateRequest) = webCallSync(callName = "updateVisit") {
        apiService.updateVisit(visitUpdateRequest)
    }

    override suspend fun createVisit(visitCreateRequest: VisitCreateRequest) = webCallSync(callName = "createVisit") {
        apiService.createVisit(visitCreateRequest)
    }

    override suspend fun getLicenses(getLicensesRequest: GetLicensesRequest): LicenseResponse = webCallSync(callName = "getLicenses") {
        apiService.getLicenses(getLicensesRequest)
    }

    override suspend fun releaseLicenses(releaseLicensesRequest: ReleaseLicensesRequest) = webCallSync(callName = "releaseLicenses") {
        apiService.releaseLicenses(releaseLicensesRequest)
    }

    override suspend fun reportSyncComplete(syncCompleteRequest: SyncCompleteRequest) = webCallSync(callName = "reportSyncComplete") {
        apiService.reportSyncComplete(syncCompleteRequest)
    }

    override suspend fun getParticipantsByUuids(getParticipantsByUuidsRequest: GetParticipantsByUuidsRequest): List<ParticipantSyncRecord> =
        webCallSync(callName = "getParticipantsByUuids") {
            apiService.getParticipantsByUuids(getParticipantsByUuidsRequest)
        }

    override suspend fun getImagesByUuids(getImagesByUuidsRequest: GetImagesByUuidsRequest): List<ParticipantImageSyncRecord> = webCallSync(callName = "getParticipantsByUuids") {
        apiService.getImagesByUuids(getImagesByUuidsRequest)
    }

    override suspend fun getVisitsByUuids(getVisitsByUuidsRequest: GetVisitsByUuidsRequest): List<VisitSyncRecord> = webCallSync(callName = "getParticipantsByUuids") {
        apiService.getVisitsByUuids(getVisitsByUuidsRequest)
    }

    override suspend fun getBiometricsTemplatesByUuids(getBiometricsTemplatesByUuidsRequest: GetBiometricsTemplatesByUuidsRequest): List<ParticipantBiometricsTemplateSyncRecord> =
        webCallSync(callName = "getParticipantsByUuids") {
            apiService.getBiometricsTemplatesByUuids(getBiometricsTemplatesByUuidsRequest)
        }

    override suspend fun uploadSyncErrors(syncErrorsRequest: SyncErrorsRequest) = webCallSync(callName = "uploadSyncErrors") {
        apiService.uploadSyncErrors(syncErrorsRequest)
    }

    override suspend fun markSyncErrorsResolved(markSyncErrorsResolved: MarkSyncErrorsResolvedRequest) = webCallSync(callName = "markSyncErrorsSolved") {
        apiService.markSyncErrorsResolved(markSyncErrorsResolved)
    }

    override suspend fun getVaccineSchedule(): VaccineSchedule = webCallSync(callName = "vaccineSchedule") {
        apiService.getVaccineSchedule()
    }

    override suspend fun getSubstancesConfig(): SubstancesConfig = webCallSync(callName = "getSubstancesConfig") {
        apiService.getSubstancesConfig()
    }

    override suspend fun getPersonImage(personUuid: String): ResponseBody? = webCallSync(callName = "getPersonImage") {
        apiService.getPersonImage(personUuid)
    }

    override suspend fun getParticipantVisitDetails(participantUuid: String): List<VisitDetailDto> = webCallSync(callName = "getParticipantVisitDetails") {
        apiService.getParticipantVisitDetails(participantUuid)
    }

    override suspend fun matchParticipants(participantId: String?, phone: String?, biometricsTemplateFile: BiometricsTemplateBytes?, country: String): List<ParticipantMatchDto> {
        val irisTemplateFormData = biometricsTemplateFile?.let { getBiometricsTemplateBytesMultiPart(it) }
        val participantIdFormData = participantId?.takeIf { it.isNotBlank() }?.let { getParticipantIdPart(it) }
        val phoneFormData = phone?.takeIf { it.isNotBlank() }?.let { getPhonePart(it) }
        val countryFormData = getCountryPart(country)
        return webCallSync(callName = "matchParticipants") {
            apiService.matchParticipants(
                irisTemplatePart = irisTemplateFormData,
                participantIdPart = participantIdFormData,
                phonePart = phoneFormData,
                countryPart = countryFormData
            )
        }
    }

    protected suspend fun tryLoginWithStoredCredentials() = mutex.withLock {
        logInfo("refreshSession")
        if (syncCookieRepository.validSessionCookieExists()) {
            logWarn("skipping refresh session 'cause already logged in")
        } else {
            val credentials = syncUserCredentialsRepository.getSyncUserCredentials()
            val loginResponse = login(credentials)
            val user = loginResponse.user?.display
            val session = loginResponse.sessionId
            val isAuthenticated = loginResponse.authenticated
            logInfo("refreshSession: $user $session $isAuthenticated")
        }
    }

    override suspend fun personTemplate(participantUuid: String, biometricsTemplate: BiometricsTemplateBytes) = webCallSync(callName = "personTemplate") {
        apiService.personTemplate(participantUuid = participantUuid, getBiometricsTemplateBytesMultiPart(biometricsTemplate))
    }

    protected suspend fun <T> webCallSync(
        requireAuthentication: Boolean = true,
        callName: String = "",
        block: suspend () -> T,
    ): T {
        return webCallUtil.webCallSync(
            requireAuthentication = requireAuthentication,
            callName = callName, block = block, refreshSession = ::tryLoginWithStoredCredentials
        )
    }

    override suspend fun getDeviceName(deviceNameRequest: DeviceNameRequest): DeviceNameResponse = webCallSync(callName = "getDeviceName") {
        apiService.getDeviceName(deviceNameRequest)
    }

    override suspend fun getLatestVersion(): VersionDto = webCallSync(callName = "getLatestVersion") {
        apiService.getLatestVersion()
    }
}