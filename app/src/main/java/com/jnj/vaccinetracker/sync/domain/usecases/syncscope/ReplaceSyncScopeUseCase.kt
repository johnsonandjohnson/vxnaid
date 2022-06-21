package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.data.database.repositories.SyncScopeRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.exceptions.ReplaceSyncScopeException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import javax.inject.Inject

class ReplaceSyncScopeUseCase @Inject constructor(
    private val syncScopeRepository: SyncScopeRepository,
    private val deleteAllImagesUseCase: DeleteAllImagesUseCase,
    private val deleteAllBiometricsTemplatesUseCase: DeleteAllBiometricsTemplatesUseCase,
    private val deleteAllVisitsUseCase: DeleteAllVisitsUseCase,
    private val deleteAllParticipantsUseCase: DeleteAllParticipantsUseCase,
    private val transactionRunner: ParticipantDbTransactionRunner,
) {

    private suspend fun wipeAllLocalData(deleteUploadedDrafts: Boolean, deleteImages: Boolean) {
        logInfo("wipeAllLocalData deleteUploadedDrafts=$deleteUploadedDrafts deleteImages=$deleteImages")
        // order is important here
        if (deleteImages)
            deleteAllImagesUseCase.deleteAllImages(deleteUploadedDrafts = deleteUploadedDrafts)
        deleteAllBiometricsTemplatesUseCase.deleteAllBiometricsTemplates(deleteUploadedDrafts = deleteUploadedDrafts)
        deleteAllVisitsUseCase.deleteAllVisits(deleteUploadedDrafts = deleteUploadedDrafts)
        deleteAllParticipantsUseCase.deleteAllParticipants(deleteUploadedDrafts = deleteUploadedDrafts)
    }

    /**
     * @return whether any database changes have occurred
     */
    private suspend fun migrateSyncData(existingSyncScope: SyncScope, newSyncScope: SyncScope): Boolean {
        if (existingSyncScope.isIdenticalTo(newSyncScope)) {
            throw ReplaceSyncScopeException("trying to replace existing syncScope [$existingSyncScope]" +
                    " with identical sync scope [$newSyncScope]")
        }
        val siteUuidChanged = existingSyncScope.siteUuid != newSyncScope.siteUuid
        val levelChanged = existingSyncScope.level != newSyncScope.level
        if (levelChanged) {
            logInfo("sync scope level changed so wiping local data")
            wipeAllLocalData(deleteUploadedDrafts = true, deleteImages = siteUuidChanged)
        } else {
            val isWithinBounds = existingSyncScope.isWithinBounds(newSyncScope)
            logInfo("existingSyncScope within bounds of new sync scope -> $isWithinBounds")
            val deleteImages: Boolean = siteUuidChanged
            if (isWithinBounds) {
                if (siteUuidChanged) {
                    logInfo("sync scope level not changed and within bounds so only deleting images")
                    deleteAllImagesUseCase.deleteAllImages(deleteUploadedDrafts = true)
                } else {
                    // nothing important has changed, skipping migration
                    logInfo("nothing important has changed between new and old sync scopes, deleting existing [existingSyncScope=$existingSyncScope, newSyncScope=$newSyncScope]")
                    syncScopeRepository.deleteExisting()
                    return false
                }
            } else {
                logInfo("sync scope level not changed but sync scope out of bounds based on ${newSyncScope.level} so wiping all data")
                wipeAllLocalData(deleteUploadedDrafts = true, deleteImages = deleteImages)
            }
        }
        logInfo("sync scope migration completed, deleting existing")
        syncScopeRepository.deleteExisting()
        return true
    }

    suspend fun replaceSyncScope(syncScope: SyncScope) = transactionRunner.withTransaction {
        val existingSyncScope = syncScopeRepository.findOne()
        val dateCreated = if (existingSyncScope != null) {
            val changedOccurred = migrateSyncData(existingSyncScope = existingSyncScope, newSyncScope = syncScope)
            if (changedOccurred) syncScope.dateCreated else existingSyncScope.dateCreated
        } else syncScope.dateCreated

        syncScopeRepository.insert(syncScope.copy(dateCreated = dateCreated))
    }
}