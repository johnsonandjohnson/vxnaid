package com.jnj.vaccinetracker.sync.domain.usecases.failed

import com.jnj.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import javax.inject.Inject

class GetFailedSyncRecordDownloadCountUseCase @Inject constructor(private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository) {

    suspend fun count(syncEntityType: SyncEntityType) = failedSyncRecordDownloadRepository.count(syncEntityType)
}