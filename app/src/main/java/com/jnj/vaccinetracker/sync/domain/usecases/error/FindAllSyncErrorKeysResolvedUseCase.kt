package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class FindAllSyncErrorKeysResolvedUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    suspend fun findAllSyncErrorKeysResolved(limit: Int): List<String> {
        require(limit > 0)
        return syncErrorRepository.findAllSyncErrorKeysByErrorState(SyncErrorState.RESOLVED, limit)
    }
}