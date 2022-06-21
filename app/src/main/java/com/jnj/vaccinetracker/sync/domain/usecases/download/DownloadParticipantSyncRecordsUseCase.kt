package com.jnj.vaccinetracker.sync.domain.usecases.download

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.data.models.ParticipantSyncRecord
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
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantSyncRecordUseCase
import javax.inject.Inject

class DownloadParticipantSyncRecordsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val storeParticipantSyncRecordUseCase: StoreParticipantSyncRecordUseCase,
    override val syncLogger: SyncLogger, override val validateSyncResponseUseCase: ValidateSyncResponseUseCase,
    override val storeFailedSyncRecordDownloadUseCase: StoreFailedSyncRecordDownloadUseCase,
    override val deleteFailedSyncRecordUseCase: DeleteFailedSyncRecordUseCase,
) : DownloadSyncRecordsUseCaseBase<ParticipantSyncRecord>() {
    override val syncEntityType: SyncEntityType
        get() = SyncEntityType.PARTICIPANT

    override suspend fun storeSyncRecord(record: ParticipantSyncRecord) {
        storeParticipantSyncRecordUseCase.store(record)
    }

    override suspend fun fetchRemoteSyncRecords(syncRequest: SyncRequest): SyncResponse<ParticipantSyncRecord> {
        return api.getAllParticipants(syncRequest)
    }

    override fun mapRecordToFailedSyncRecordDownload(record: ParticipantSyncRecord, dateLastDownloadAttempt: DateEntity): FailedSyncRecordDownload {
        return record.toFailedSyncRecordDownload(dateLastDownloadAttempt)
    }
}