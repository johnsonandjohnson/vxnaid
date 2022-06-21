package com.jnj.vaccinetracker.sync.domain.usecases.store

import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.VisitSyncRecord
import com.jnj.vaccinetracker.sync.data.models.VisitSyncRecord.Delete.Companion.toDomain
import com.jnj.vaccinetracker.sync.data.models.VisitSyncRecord.Update.Companion.toDomain
import com.jnj.vaccinetracker.sync.domain.entities.ParticipantPendingCall
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.store.base.StoreSyncRecordUseCaseBase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreVisitSyncRecordUseCase @Inject constructor(
    private val visitRepository: VisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
    private val syncLogger: SyncLogger,
) : StoreSyncRecordUseCaseBase<VisitSyncRecord, Visit> {

    override suspend fun store(syncRecord: VisitSyncRecord) = transactionRunner.withTransaction {
        when (syncRecord) {
            is VisitSyncRecord.Delete -> delete(syncRecord)
            is VisitSyncRecord.Update -> update(syncRecord)
        }.let {}
    }

    private suspend fun onInsertSuccess(visit: Visit) {
        val visitUuid = visit.visitUuid
        logInfo("onInsertSuccess: $visitUuid")

        deleteUploadedDraftVisit(visit)
        deleteUploadedDraftVisitEncounter(visit)
    }

    private suspend fun update(syncRecord: VisitSyncRecord.Update) {
        val visit = syncRecord.toDomain()
        visitRepository.insert(visit, orReplace = true)
        onInsertSuccess(visit)
    }

    /**
     * Deletes a specific visit from the local table
     */
    private suspend fun delete(syncRecord: VisitSyncRecord.Delete) {
        visitRepository.deleteByVisitUuid(syncRecord.visitUuid)
        draftVisitEncounterRepository.deleteByVisitUuid(syncRecord.visitUuid, draftState = DraftState.UPLOADED)
        draftVisitRepository.deleteByVisitUuid(syncRecord.visitUuid, draftState = DraftState.UPLOADED)
        deletedSyncRecordRepository.insert(syncRecord.toDomain(), orReplace = true)
    }

    private suspend fun deleteUploadedDraftVisitEncounter(visit: Visit) {
        val visitUuid = visit.visitUuid
        val draftState = draftVisitRepository.findDraftStateByVisitUuid(visitUuid = visitUuid)
        suspend fun deleteVisit() {
            val isRecordDeleted = draftVisitRepository.deleteByVisitUuid(visitUuid)
            logInfo("delete draft visit [draftState:$draftState, isRecordDeleted:$isRecordDeleted]")
        }
        when (draftState) {
            DraftState.UPLOADED -> {
                deleteVisit()
            }
            DraftState.UPLOAD_PENDING -> {
                logInfo("Matching visit with local draft visit")
                val draftVisit = draftVisitRepository.findByVisitUuid(visitUuid)
                if (draftVisit != null) {
                    if (draftVisit.participantUuid == visit.participantUuid) {
                        logInfo("Remote visit is matched to local visit")
                        deleteVisit()
                        val syncError = SyncErrorMetadata.UploadParticipantPendingCall(
                            ParticipantPendingCall.Type.CREATE_VISIT,
                            participantUuid = draftVisit.participantUuid,
                            visitUuid = draftVisit.visitUuid,
                            locationUuid = draftVisit.locationUuid,
                            participantId = null
                        )
                        syncLogger.clearSyncError(syncError)
                    } else {
                        logInfo("Remote visit not matching")
                    }
                } else {
                    logInfo("draft visit is null $visitUuid")
                }
            }
            null -> logInfo("nothing deleted for visit $visitUuid [$draftState]")
        }.let { }
    }

    private suspend fun deleteUploadedDraftVisit(visit: Visit) {
        val visitUuid = visit.visitUuid
        val draftState = draftVisitEncounterRepository.findDraftStateByVisitUuid(visitUuid = visitUuid)
        suspend fun deleteEncounter() {
            val isRecordDeleted = draftVisitEncounterRepository.deleteByVisitUuid(visitUuid)
            logInfo("delete draft encounter [draftState:$draftState, isRecordDeleted:$isRecordDeleted]")
        }
        when (draftState) {
            DraftState.UPLOADED -> {
                deleteEncounter()
            }
            DraftState.UPLOAD_PENDING -> {
                logInfo("Matching encounter with local draft encounter")
                val draftVisitEncounter = draftVisitEncounterRepository.findByVisitUuid(visitUuid)

                if (draftVisitEncounter != null) {
                    val isSameParticipantUuid = draftVisitEncounter.participantUuid == visit.participantUuid

                    val visitEncounters = visit.observations.values
                    val draftEncounters = draftVisitEncounter.observationsWithDate.values
                    val listDifference = visitEncounters - draftEncounters.toSet()

                    if (listDifference.isEmpty() && isSameParticipantUuid) {
                        logInfo("Remote encounter is matched to local encounter")
                        deleteEncounter()
                        val syncError = SyncErrorMetadata.UploadParticipantPendingCall(
                            ParticipantPendingCall.Type.UPDATE_VISIT,
                            participantUuid = draftVisitEncounter.participantUuid,
                            visitUuid = draftVisitEncounter.visitUuid,
                            locationUuid = draftVisitEncounter.locationUuid,
                            participantId = null
                        )
                        syncLogger.clearSyncError(syncError)
                    } else {
                        logInfo("Remote encounter not matching")
                    }
                } else {
                    logInfo("draftVisitEncounter is null $visitUuid")
                }
            }
            null -> logInfo("nothing deleted for encounter $visitUuid [$draftState]")
        }.let { }
    }

}