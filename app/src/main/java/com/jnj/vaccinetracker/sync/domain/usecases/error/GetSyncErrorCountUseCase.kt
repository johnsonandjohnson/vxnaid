package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class GetSyncErrorCountUseCase @Inject constructor(private val syncErrorRepository: SyncErrorRepository) {

    suspend fun syncErrorCount(): Long = syncErrorRepository.countByErrorStates(SyncErrorState.statesNotResolved())

}