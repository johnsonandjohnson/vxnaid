package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import javax.inject.Inject

class DeleteAllSyncErrorsUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {


    suspend fun deleteAllSyncErrors() {
        syncErrorRepository.deleteAll()
    }
}