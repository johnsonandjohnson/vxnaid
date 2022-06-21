package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncError
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class FindAllSyncErrorsPendingUploadUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    private companion object {
        private const val LIMIT_INTERNAL = 50
    }

    suspend fun findAllSyncErrorsPendingUpload(limit: Int = LIMIT_INTERNAL): List<SyncError> {
        require(limit > 0)
        val errors = mutableListOf<SyncError>()
        while (errors.size < limit) {
            val results = syncErrorRepository.findAllByErrorStates(listOf(SyncErrorState.PENDING_UPLOAD), errors.size, limit.coerceAtMost(LIMIT_INTERNAL))
            if (results.isEmpty())
                break
            else {
                errors += results
            }
        }
        return errors
    }
}