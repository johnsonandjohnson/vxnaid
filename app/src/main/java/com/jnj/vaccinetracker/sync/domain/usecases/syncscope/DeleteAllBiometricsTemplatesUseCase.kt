package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.domain.usecases.syncscope.base.DeleteDataFileUseCaseBase
import javax.inject.Inject

class DeleteAllBiometricsTemplatesUseCase @Inject constructor(
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    override val participantDataFileIO: ParticipantDataFileIO,
    private val deleteAllFailedSyncRecordsUseCase: DeleteAllFailedSyncRecordsUseCase,
    private val deleteAllDeletedSyncRecordsUseCase: DeleteAllDeletedSyncRecordsUseCase,
) : DeleteDataFileUseCaseBase() {

    private val syncEntityType = SyncEntityType.BIOMETRICS_TEMPLATE

    suspend fun deleteAllBiometricsTemplates(deleteUploadedDrafts: Boolean) {
        logInfo("deleteAllBiometricsTemplates $deleteUploadedDrafts")
        deleteAllFailedSyncRecordsUseCase.deleteAll(syncEntityType)
        deleteAllDeletedSyncRecordsUseCase.deleteAll(syncEntityType)
        participantDataFileIO.deleteAllSyncBiometricsTemplates()
        participantBiometricsTemplateRepository.deleteAll()
        if (deleteUploadedDrafts) {
            deleteFilesQuery { offset, limit ->
                draftParticipantBiometricsTemplateRepository.findAllUploaded(offset, limit)
            }
            draftParticipantBiometricsTemplateRepository.deleteAllUploaded()
        }
    }
}