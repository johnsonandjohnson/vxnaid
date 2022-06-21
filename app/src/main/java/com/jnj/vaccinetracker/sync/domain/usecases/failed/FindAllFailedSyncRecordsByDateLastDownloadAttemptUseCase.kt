package com.jnj.vaccinetracker.sync.domain.usecases.failed

import com.jnj.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import javax.inject.Inject

class FindAllFailedSyncRecordsByDateLastDownloadAttemptUseCase @Inject constructor(private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository) {

    suspend fun findAllByDateLastDownloadAttemptLesserThan(syncEntityType: SyncEntityType, date: SyncDate, limit: Int): List<FailedSyncRecordDownload> {
        return failedSyncRecordDownloadRepository.findAllByDateLastDownloadAttemptLesserThan(syncEntityType, date.date, 0, limit)
    }
}