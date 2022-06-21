package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.jnj.vaccinetracker.sync.data.models.MarkSyncErrorsResolvedRequest
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class UploadResolvedSyncErrorKeysUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val syncErrorRepository: SyncErrorRepository,
) {

    /**
     * mark [syncErrorKeys] as resolved in backend.
     *
     * **Delete locally afterwards:**
     *
     * It's possible some of the respective rows to the [syncErrorKeys] have been replaced while we were uploading.
     * The rows for these cases will not be deleted.
     */
    suspend fun uploadResolved(syncErrorKeys: List<String>) {
        api.markSyncErrorsResolved(MarkSyncErrorsResolvedRequest(syncErrorKeys))
        syncErrorRepository.deleteByKeysAndErrorState(syncErrorKeys, SyncErrorState.RESOLVED)
    }
}