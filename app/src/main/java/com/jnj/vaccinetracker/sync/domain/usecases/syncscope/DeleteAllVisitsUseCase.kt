package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class DeleteAllVisitsUseCase @Inject constructor(
    private val visitRepository: VisitRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val deleteAllFailedSyncRecordsUseCase: DeleteAllFailedSyncRecordsUseCase,
    private val deleteAllDeletedSyncRecordsUseCase: DeleteAllDeletedSyncRecordsUseCase,
) {

    private val syncEntityType = SyncEntityType.VISIT
    suspend fun deleteAllVisits(deleteUploadedDrafts: Boolean) {
        logInfo("deleteAllVisits $deleteUploadedDrafts")
        deleteAllFailedSyncRecordsUseCase.deleteAll(syncEntityType)
        deleteAllDeletedSyncRecordsUseCase.deleteAll(syncEntityType)
        visitRepository.deleteAll()
        if (deleteUploadedDrafts) {
            // normally these are already deleted
            draftVisitRepository.deleteAllUploaded()
            draftVisitEncounterRepository.deleteAllUploaded()
        }
    }
}