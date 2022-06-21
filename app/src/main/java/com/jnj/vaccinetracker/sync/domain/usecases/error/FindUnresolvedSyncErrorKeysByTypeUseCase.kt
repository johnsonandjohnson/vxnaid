package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class FindUnresolvedSyncErrorKeysByTypeUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    suspend fun findUnresolvedSyncErrorKeysByType(type: String): List<String> {
        return syncErrorRepository.findAllSyncErrorKeysByType(SyncErrorState.statesNotResolved(), type)
    }
}