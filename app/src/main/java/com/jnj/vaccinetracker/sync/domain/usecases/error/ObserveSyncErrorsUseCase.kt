package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSyncErrorsUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    fun observeChanges(): Flow<Long> {
        return syncErrorRepository.observeChanges()
    }
}