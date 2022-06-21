package com.jnj.vaccinetracker.sync.domain.entities

import android.content.Context
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.di.AppResources
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import java.text.SimpleDateFormat
import java.util.*

data class SyncErrorOverview(val metadata: SyncErrorMetadata, val dateCreated: DateEntity) : SyncErrorOverviewDisplay() {
    val key get() = metadata.key

    private fun SyncEntityType.display(context: ResourcesWrapper) = when (this) {
        SyncEntityType.PARTICIPANT -> R.string.participant
        SyncEntityType.IMAGE -> R.string.image
        SyncEntityType.BIOMETRICS_TEMPLATE -> R.string.biometric_template
        SyncEntityType.VISIT -> R.string.visit
    }.let { context.getString(it) }

    private fun ParticipantPendingCall.Type.display(context: ResourcesWrapper) = when (this) {
        ParticipantPendingCall.Type.REGISTER_PARTICIPANT -> R.string.participant
        ParticipantPendingCall.Type.CREATE_VISIT -> R.string.visit
        ParticipantPendingCall.Type.UPDATE_VISIT -> R.string.encounter
    }.let { context.getString(it) }

    private fun MasterDataFile.display(context: ResourcesWrapper) = when (this) {
        MasterDataFile.CONFIGURATION -> R.string.configuration
        MasterDataFile.SITES -> R.string.sites
        MasterDataFile.LOCALIZATION -> R.string.localization
        MasterDataFile.ADDRESS_HIERARCHY -> R.string.address_hierarchy
        MasterDataFile.VACCINE_SCHEDULE -> R.string.vaccine_schedule
    }.let { context.getString(it) }

    private fun LicenseType.display(context: ResourcesWrapper) = when (this) {
        LicenseType.IRIS_CLIENT -> R.string.iris_license_client
        LicenseType.IRIS_MATCHING -> R.string.iris_license_matching
    }.let { context.getString(it) }

    override fun displayErrorMessage(context: ResourcesWrapper): String = with(metadata) {
        when (this) {
            is SyncErrorMetadata.StoreSyncRecord ->
                if (visitUuid == null)
                    context.getString(R.string.store_participant_sync_record_error, syncEntityType.display(context), participantUuid)
                else
                    context.getString(R.string.store_visit_sync_record_error, syncEntityType.display(context), participantUuid, visitUuid)
            is SyncErrorMetadata.FindAllRelatedDraftDataPendingUpload -> context.getString(R.string.find_all_drafts_pending_upload_error)
            is SyncErrorMetadata.License -> when (action) {
                SyncErrorMetadata.License.Action.GET_LICENSE_CALL -> R.string.get_license_call_error
                SyncErrorMetadata.License.Action.RELEASE_LICENSE_CALL -> R.string.release_license_call_error
                SyncErrorMetadata.License.Action.ACTIVATE_LICENSE -> R.string.activate_license_call_error
                SyncErrorMetadata.License.Action.DEACTIVATE_LICENSE -> R.string.deactivate_license_call_error
                SyncErrorMetadata.License.Action.OBTAIN_ACTIVATED_LICENSE -> R.string.obtain_license_call_error
            }.let { context.getString(it, licenseType.display(context)) }
            is SyncErrorMetadata.MasterData -> when (action) {
                SyncErrorMetadata.MasterData.Action.GET_MASTER_DATA_CALL -> R.string.get_master_data_call_error
                SyncErrorMetadata.MasterData.Action.READ_PERSISTED_MASTER_DATA -> R.string.read_persisted_master_data_error
                SyncErrorMetadata.MasterData.Action.PERSIST_MASTER_DATA -> R.string.persist_master_data_error
                SyncErrorMetadata.MasterData.Action.MAP_MASTER_DATA -> R.string.map_master_data_error
            }.let { context.getString(it, masterDataFile.display(context)) }
            is SyncErrorMetadata.MasterDataUpdatesCall -> context.getString(R.string.get_master_data_updates_call_error)
            is SyncErrorMetadata.UploadParticipantPendingCall ->
                when {
                    visitUuid != null -> context.getString(R.string.upload_visit_pending_call_error, pendingCallType.display(context), participantUuid, visitUuid)
                    participantId != null -> context.getString(R.string.upload_participant_pending_call_error_with_participant_id, pendingCallType.display(context), participantId)
                    else -> context.getString(R.string.upload_participant_pending_call_error, pendingCallType.display(context), participantUuid)
                }
            is SyncErrorMetadata.GetAllSyncRecordsCallValidation -> context.getString(R.string.get_all_sync_records_validation_error, syncEntityType.display(context))
            is SyncErrorMetadata.GetAllSyncRecordsCall -> context.getString(R.string.get_all_sync_records_call_error, syncEntityType.display(context))
            is SyncErrorMetadata.ReportSyncCompletedDateCall -> context.getString(R.string.report_sync_completed_date_error)
            is SyncErrorMetadata.GetSyncRecordsByUuidsCall -> context.getString(R.string.get_sync_records_by_uuids_call_error)
            is SyncErrorMetadata.DeviceNameCall -> context.getString(R.string.get_device_name_by_site_uuid_call_error)
            is SyncErrorMetadata.UploadBiometricsTemplate -> context.getString(R.string.upload_biometrics_template_error, participantUuid)
        }
    }

    override val displayDate: String get() = dateFormat.format(dateCreated)
}

abstract class SyncErrorOverviewDisplay {
    abstract val displayDate: String
    abstract fun displayErrorMessage(context: ResourcesWrapper): String
    fun displayErrorMessage(context: Context): String {
        return displayErrorMessage(AppResources(context))
    }
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)