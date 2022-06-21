package com.jnj.vaccinetracker.fake.data.network

import com.jnj.vaccinetracker.common.data.models.ParticipantMatchDto
import com.jnj.vaccinetracker.common.data.models.api.request.RegisterParticipantRequest
import com.jnj.vaccinetracker.common.data.models.api.request.VisitCreateRequest
import com.jnj.vaccinetracker.common.data.models.api.request.VisitUpdateRequest
import com.jnj.vaccinetracker.common.data.models.api.response.*
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.sync.data.models.*
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import okhttp3.ResponseBody
import javax.inject.Inject

class VaccineTrackerSyncApiDataSourceFake @Inject constructor(
    private val fakeBackendApi: FakeBackendApi,
    private val vaccineTrackerSyncApiDataSourceDefault: VaccineTrackerSyncApiDataSourceDefault,
) : VaccineTrackerSyncApiDataSource by vaccineTrackerSyncApiDataSourceDefault {
    override suspend fun getAllParticipants(syncRequest: SyncRequest): ParticipantSyncResponse {
        return fakeBackendApi.getAllParticipants(syncRequest)
    }

    override suspend fun getAllParticipantBiometricsTemplates(syncRequest: SyncRequest): ParticipantBiometricsTemplateSyncResponse {
        return fakeBackendApi.getAllParticipantBiometricsTemplates(syncRequest)
    }

    override suspend fun getAllParticipantImages(syncRequest: SyncRequest): ParticipantImageSyncResponse {
        return fakeBackendApi.getAllParticipantImages(syncRequest)
    }

    override suspend fun getAllVisits(syncRequest: SyncRequest): VisitSyncResponse {
        return fakeBackendApi.getAllVisits(syncRequest)
    }

    override suspend fun getMasterDataUpdates(): MasterDataUpdatesResponse {
        return fakeBackendApi.getMasterDataUpdates()
    }

    override suspend fun getConfiguration(): ConfigurationDto {
        return fakeBackendApi.getConfiguration()
    }

    override suspend fun getLocalization(): LocalizationMapDto {
        return fakeBackendApi.getLocalization()
    }

    override suspend fun getSites(): SitesDto {
        return fakeBackendApi.getSites()
    }

    override suspend fun getActiveUsers(): ActiveUsersResponse {
        return fakeBackendApi.getActiveUsers()
    }

    override suspend fun registerParticipant(registerParticipantRequest: RegisterParticipantRequest, biometricsTemplate: BiometricsTemplateBytes?): RegisterParticipantResponse {
        return fakeBackendApi.registerParticipant(registerParticipantRequest, biometricsTemplate)
    }

    override suspend fun updateVisit(visitUpdateRequest: VisitUpdateRequest) {
        return fakeBackendApi.updateVisit(visitUpdateRequest)
    }

    override suspend fun createVisit(visitCreateRequest: VisitCreateRequest) {
        return fakeBackendApi.createVisit(visitCreateRequest)
    }

    override suspend fun getVisitsByUuids(getVisitsByUuidsRequest: GetVisitsByUuidsRequest): List<VisitSyncRecord> {
        return emptyList()
    }

    override suspend fun getBiometricsTemplatesByUuids(getBiometricsTemplatesByUuidsRequest: GetBiometricsTemplatesByUuidsRequest): List<ParticipantBiometricsTemplateSyncRecord> {
        return emptyList()
    }

    override suspend fun getImagesByUuids(getImagesByUuidsRequest: GetImagesByUuidsRequest): List<ParticipantImageSyncRecord> {
        return emptyList()
    }

    override suspend fun getParticipantsByUuids(getParticipantsByUuidsRequest: GetParticipantsByUuidsRequest): List<ParticipantSyncRecord> {
        return emptyList()
    }

    override suspend fun markSyncErrorsResolved(markSyncErrorsResolved: MarkSyncErrorsResolvedRequest) {
        //no-op
    }

    override suspend fun uploadSyncErrors(syncErrorsRequest: SyncErrorsRequest) {
        //no-op
    }

    override suspend fun getCountryAddressHierarchy(): List<String> = fakeBackendApi.getCountryAddressHierarchy()

    override suspend fun reportSyncComplete(syncCompleteRequest: SyncCompleteRequest) {
        fakeBackendApi.reportSyncComplete(syncCompleteRequest)
    }

    override suspend fun getVaccineSchedule(): VaccineSchedule {
        return fakeBackendApi.getVaccineSchedule()
    }

    override suspend fun getParticipantVisitDetails(participantUuid: String): List<VisitDetailDto> = fakeBackendApi.getParticipantVisitDetails(participantUuid)

    override suspend fun getPersonImage(personUuid: String): ResponseBody? = fakeBackendApi.getPersonImage(personUuid)

    override suspend fun matchParticipants(participantId: String?, phone: String?, biometricsTemplateFile: BiometricsTemplateBytes?, country: String): List<ParticipantMatchDto> =
        fakeBackendApi.matchParticipants(participantId = participantId, phone = phone, biometricsTemplateBytes = biometricsTemplateFile)
}