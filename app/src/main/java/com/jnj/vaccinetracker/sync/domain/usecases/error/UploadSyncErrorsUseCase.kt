package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.data.models.SyncErrorsRequest
import com.jnj.vaccinetracker.sync.data.models.toDto
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.entities.SyncError
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class UploadSyncErrorsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val syncErrorRepository: SyncErrorRepository,
) {

    suspend fun upload(syncErrors: List<SyncError>) {
        api.uploadSyncErrors(SyncErrorsRequest(syncErrors.map { it.toDto() }))
        syncErrorRepository.updateSyncErrorState(SyncErrorState.UPLOADED, syncErrors.map { it.metadata })
    }
}