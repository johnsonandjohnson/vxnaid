package com.jnj.vaccinetracker.sync.data.network


import com.jnj.vaccinetracker.common.data.models.ParticipantMatchDto
import com.jnj.vaccinetracker.common.data.models.api.request.*
import com.jnj.vaccinetracker.common.data.models.api.response.*
import com.jnj.vaccinetracker.common.domain.entities.Substance
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.sync.data.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VaccineTrackerSyncApiService {
    companion object {
        private const val BIOMETRIC = "openmrs/ws/rest/v1/biometric"
    }

    @POST("$BIOMETRIC/sync/getAllParticipants")
    suspend fun getAllParticipants(@Body syncRequest: SyncRequest): ParticipantSyncResponse

    @POST("$BIOMETRIC/sync/getAllParticipantBiometricsTemplates")
    suspend fun getAllParticipantBiometricsTemplates(@Body syncRequest: SyncRequest): ParticipantBiometricsTemplateSyncResponse

    @POST("$BIOMETRIC/sync/getAllParticipantImages")
    suspend fun getAllParticipantImages(@Body syncRequest: SyncRequest): ParticipantImageSyncResponse

    @POST("$BIOMETRIC/sync/getAllVisits")
    suspend fun getAllVisits(@Body syncRequest: SyncRequest): VisitSyncResponse

    @GET("$BIOMETRIC/sync/config-updates")
    suspend fun getMasterDataUpdates(): MasterDataUpdatesResponse

    @GET("openmrs/ws/rest/v1/session")
    suspend fun login(@Header("Authorization") basicAuth: String): LoginResponse

    @GET("$BIOMETRIC/config/main")
    suspend fun getConfiguration(): ConfigurationDto

    @GET("$BIOMETRIC/config/localization")
    suspend fun getLocalization(): LocalizationMapDto

    @GET("$BIOMETRIC/config/vaccine-schedule")
    suspend fun getVaccineSchedule(): VaccineSchedule

    @GET("$BIOMETRIC/config/substances")
    suspend fun getSubstancesConfig(): SubstancesConfig

    @GET("$BIOMETRIC/config/substanceGroups")
    suspend fun getSubstancesGroupConfig(): SubstancesGroupConfig

    @GET("$BIOMETRIC/location")
    suspend fun getSites(): SitesDto

    @GET("$BIOMETRIC/users")
    suspend fun getActiveUsers(): ActiveUsersResponse

    @Multipart
    @POST("$BIOMETRIC/register")
    suspend fun registerParticipant(
        @Part("biographicData") registerParticipantRequest: RegisterParticipantRequest,
        @Part irisTemplatePart: MultipartBody.Part?,
    ): RegisterParticipantResponse


    @POST("$BIOMETRIC/encounter")
    suspend fun updateVisit(@Body visitUpdateRequest: VisitUpdateRequest)

    @POST("$BIOMETRIC/visit")
    suspend fun createVisit(@Body visitCreateRequest: VisitCreateRequest)

    @GET("$BIOMETRIC/addresshierarchy")
    suspend fun getCountryAddressHierarchy(): List<String>

    @POST("$BIOMETRIC/license")
    suspend fun getLicenses(@Body getLicensesRequest: GetLicensesRequest): LicenseResponse

    @POST("$BIOMETRIC/license/release")
    suspend fun releaseLicenses(@Body releaseLicensesRequest: ReleaseLicensesRequest)

    @POST("$BIOMETRIC/sync")
    suspend fun reportSyncComplete(@Body syncCompleteRequest: SyncCompleteRequest)

    @POST("$BIOMETRIC/getParticipantsByUuids")
    suspend fun getParticipantsByUuids(@Body getParticipantsByUuidsRequest: GetParticipantsByUuidsRequest): List<ParticipantSyncRecord>

    @POST("$BIOMETRIC/getImagesByUuids")
    suspend fun getImagesByUuids(@Body getImagesByUuidsRequest: GetImagesByUuidsRequest): List<ParticipantImageSyncRecord>

    @POST("$BIOMETRIC/getVisitsByUuids")
    suspend fun getVisitsByUuids(@Body getVisitsByUuidsRequest: GetVisitsByUuidsRequest): List<VisitSyncRecord>

    @POST("$BIOMETRIC/getBiometricTemplatesByUuids")
    suspend fun getBiometricsTemplatesByUuids(@Body getBiometricsTemplatesByUuidsRequest: GetBiometricsTemplatesByUuidsRequest): List<ParticipantBiometricsTemplateSyncRecord>

    @POST("$BIOMETRIC/sync/error")
    suspend fun uploadSyncErrors(@Body syncErrorsRequest: SyncErrorsRequest)

    @POST("$BIOMETRIC/sync/error/resolved")
    suspend fun markSyncErrorsResolved(@Body markSyncErrorsResolved: MarkSyncErrorsResolvedRequest)

    @POST("$BIOMETRIC/devicename")
    suspend fun getDeviceName(@Body deviceNameRequest: DeviceNameRequest): DeviceNameResponse

    @Multipart
    @POST("$BIOMETRIC/match")
    suspend fun matchParticipants(
        @Part irisTemplatePart: MultipartBody.Part?,
        @Part participantIdPart: MultipartBody.Part?,
        @Part phonePart: MultipartBody.Part?,
        @Part countryPart: MultipartBody.Part?,
    ): List<ParticipantMatchDto>

    @GET("$BIOMETRIC/personimage/{personUuid}")
    suspend fun getPersonImage(@Path("personUuid") personUuid: String): ResponseBody?

    @GET("$BIOMETRIC/visit/{participantUuid}")
    suspend fun getParticipantVisitDetails(@Path("participantUuid") participantUuid: String): List<VisitDetailDto>

    @Multipart
    @POST("$BIOMETRIC/persontemplate/{participantUuid}")
    suspend fun personTemplate(@Path("participantUuid") participantUuid: String, @Part irisTemplatePart: MultipartBody.Part?)

    @GET("$BIOMETRIC/version")
    suspend fun getLatestVersion(): VersionDto

    @GET("$BIOMETRIC/health")
    suspend fun getHealth(): Response<*>
}