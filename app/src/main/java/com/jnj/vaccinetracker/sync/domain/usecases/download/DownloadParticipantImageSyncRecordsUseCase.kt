package com.jnj.vaccinetracker.sync.domain.usecases.download

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.data.models.ParticipantImageSyncRecord
import com.jnj.vaccinetracker.sync.data.models.SyncRequest
import com.jnj.vaccinetracker.sync.data.models.SyncResponse
import com.jnj.vaccinetracker.sync.data.models.toFailedSyncRecordDownload
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCaseBase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantImageSyncRecordUseCase
import javax.inject.Inject

class DownloadParticipantImageSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantImageSyncRecordUseCase: StoreParticipantImageSyncRecordUseCase,
    override val syncLogger: SyncLogger, override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<ParticipantImageSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.IMAGE

    override suspend fun storeSyncRecord(record: ParticipantImageSyncRecord) {
        storeParticipantImageSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<ParticipantImageSyncRecord> {
        return api.getAllParticipantImages(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: ParticipantImageSyncRecord, dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
        return record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
    }

}