package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.logDebug
import javax.inject.Inject

class DeleteAllDeletedSyncRecordsUseCase @Inject constructor(
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
) {


    suspend fun deleteAll(syncEntityType: SyncEntityType) = deletedSyncRecordRepository.deleteAll(syncEntityType).also {
        logDebug("deleteAll $syncEntityType")
    }
}