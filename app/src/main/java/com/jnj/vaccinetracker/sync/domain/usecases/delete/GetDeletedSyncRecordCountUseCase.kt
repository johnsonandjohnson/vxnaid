package com.jnj.vaccinetracker.sync.domain.usecases.delete

import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import javax.inject.Inject

class GetDeletedSyncRecordCountUseCase @Inject constructor(private val deletedSyncRecordRepository: DeletedSyncRecordRepository) {

    suspend fun count(syncEntityType: SyncEntityType) = deletedSyncRecordRepository.count(syncEntityType)
}