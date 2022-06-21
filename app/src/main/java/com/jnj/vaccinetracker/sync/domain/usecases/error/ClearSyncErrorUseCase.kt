package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class ClearSyncErrorUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    /**
     * mark [metadata] as resolved. deletion will happen later.
     */
    suspend fun clearSyncError(metadata: SyncErrorMetadata) {
        syncErrorRepository.updateSyncErrorState(SyncErrorState.RESOLVED, listOf(metadata))
    }
}