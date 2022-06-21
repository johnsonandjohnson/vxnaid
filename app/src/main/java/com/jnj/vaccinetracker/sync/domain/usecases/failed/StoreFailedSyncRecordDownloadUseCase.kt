package com.jnj.vaccinetracker.sync.domain.usecases.failed

import com.jnj.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.jnj.vaccinetracker.sync.domain.entities.FailedSyncRecordDownload
import javax.inject.Inject

class StoreFailedSyncRecordDownloadUseCase @Inject constructor(private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository) {

    suspend fun store(failedSyncRecordDownload: FailedSyncRecordDownload) = failedSyncRecordDownloadRepository.insert(failedSyncRecordDownload, orReplace = true)
}