package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.helpers.pagingQueryList
import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorOverview
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class FindAllSyncErrorMetadataUnresolvedUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    private companion object {
        /**
         * arbitrary number, could possibly be optimized
         */
        private const val LIMIT = 2000
    }

    suspend fun findAllSyncErrorMetadataUnresolved(): List<SyncErrorOverview> {
        return pagingQueryList(pageSize = LIMIT) { offset, limit ->
            syncErrorRepository.findAllOverview(SyncErrorState.statesNotResolved(), offset, limit)
        }
    }
}