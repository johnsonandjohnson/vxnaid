package com.jnj.vaccinetracker.sync.domain.usecases.download

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.data.models.ParticipantBiometricsTemplateSyncRecord
import com.jnj.vaccinetracker.sync.data.models.SyncRequest
import com.jnj.vaccinetracker.sync.data.models.SyncResponse
import com.jnj.vaccinetracker.sync.data.models.toFailedSyncRecordDownload
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCaseBase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantBiometricsTemplateSyncRecordUseCase
import javax.inject.Inject

class DownloadParticipantBiometricsTemplateSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantBiometricsTemplateSyncRecordUseCase: StoreParticipantBiometricsTemplateSyncRecordUseCase,
    override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val syncLogger: SyncLogger, override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<ParticipantBiometricsTemplateSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.BIOMETRICS_TEMPLATE

    override suspend fun storeSyncRecord(record: ParticipantBiometricsTemplateSyncRecord) {
        storeParticipantBiometricsTemplateSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<ParticipantBiometricsTemplateSyncRecord> {
        return api.getAllParticipantBiometricsTemplates(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: ParticipantBiometricsTemplateSyncRecord, dateLastDownloadAttempt: DateEntity) =
        record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
}