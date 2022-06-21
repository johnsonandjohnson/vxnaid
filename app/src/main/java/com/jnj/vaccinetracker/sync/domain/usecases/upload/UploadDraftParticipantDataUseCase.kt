package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.domain.entities.DraftParticipant
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.sync.domain.entities.ParticipantPendingCall
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.yield
import javax.inject.Inject

class UploadDraftParticipantDataUseCase @Inject constructor(
    private val uploadDraftParticipantUseCase: UploadDraftParticipantUseCase,
    private val uploadDraftVisitUseCase: UploadDraftVisitUseCase,
    private val uploadDraftVisitEncounterUseCase: UploadDraftVisitEncounterUseCase,
    private val syncLogger: SyncLogger,
) {


    suspend fun upload(participantPendingCall: ParticipantPendingCall, logSyncErrors: Boolean): ParticipantPendingCall {
        logInfo("upload ${participantPendingCall::class.simpleName} ${participantPendingCall.participantUuid}")
        val participantId = (participantPendingCall as? ParticipantPendingCall.RegisterParticipant?)?.participantId
        return participantPendingCall.apply {
            val syncErrorMetadata =
                SyncErrorMetadata.UploadParticipantPendingCall(
                    pendingCallType = type,
                    participantUuid = participantUuid,
                    visitUuid = visitUuid,
                    locationUuid = locationUuid,
                    participantId = participantId
                )
            try {
                when (this) {
                    is ParticipantPendingCall.CreateVisit -> {
                        uploadDraftVisitUseCase.upload(draftVisit)
                    }
                    is ParticipantPendingCall.RegisterParticipant -> {
                        val draftParticipant = uploadDraftParticipantUseCase.upload(draftParticipant, true, updateDraftState = true)
                        onParticipantRegistered(draftParticipant)
                    }
                    is ParticipantPendingCall.UpdateVisit -> {
                        uploadDraftVisitEncounterUseCase.upload(draftEncounter)
                    }
                }.also { _ ->
                    syncLogger.clearSyncError(syncErrorMetadata)
                }
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                if (logSyncErrors) {
                    syncLogger.logSyncError(syncErrorMetadata, ex)
                }
                throw ex
            }
        }
    }

    private fun onParticipantRegistered(draftParticipant: DraftParticipant) {
        logInfo("onParticipantRegistered: ${draftParticipant.participantUuid}")
        draftParticipant.biometricsTemplate?.let { template ->
            if (template.draftState.isPendingUpload()) {
                logInfo("template is still pending upload, so logging sync error")
                // template was not registered so log sync error, we will retry after an hour
                val syncError = SyncErrorMetadata.UploadBiometricsTemplate(participantUuid = draftParticipant.participantUuid)
                syncLogger.logSyncError(syncError, Exception("error, isIrisRegistered is false"))
            }
        }
    }

}