package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.*
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class FindMostRecentDateModifiedOccurrenceUseCase @Inject constructor(
    private val participantImageRepository: ParticipantImageRepository,
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    private val participantRepository: ParticipantRepository,
    private val visitRepository: VisitRepository,
    private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
) {

    private suspend fun findDateModifiedSuccess(syncEntityType: SyncEntityType) = when (syncEntityType) {
        SyncEntityType.PARTICIPANT -> participantRepository.findMostRecentDateModifiedOccurrence()
        SyncEntityType.IMAGE -> participantImageRepository.findMostRecentDateModifiedOccurrence()
        SyncEntityType.BIOMETRICS_TEMPLATE -> participantBiometricsTemplateRepository.findMostRecentDateModifiedOccurrence()
        SyncEntityType.VISIT -> visitRepository.findMostRecentDateModifiedOccurrence()

    }

    private suspend fun findDateModifiedFailed(syncEntityType: SyncEntityType) = failedSyncRecordDownloadRepository.findMostRecentDateModifiedOccurrence(syncEntityType)

    private suspend fun findDateModifiedDeleted(syncEntityType: SyncEntityType) = deletedSyncRecordRepository.findMostRecentDateModifiedOccurrence(syncEntityType)

    /**
     * we'll compare most recent date modified (+occurrence) of synced records as well as records added to failed sync record download table.
     * If both dates are equal, we simply need to create a sum of the occurrence of respective dates. This value will yield the correct offset that can be passed to the backend.
     * If both dates are not equal, then only pick the [DateModifiedOccurrence] of the most recent one.
     */
    suspend fun findMostRecentDateModifiedOccurrence(syncEntityType: SyncEntityType): DateModifiedOccurrence? {
        logInfo("findMostRecentDateModifiedOccurrence: $syncEntityType")
        val dateModifiedOccurrencesInitial = listOfNotNull(
            findDateModifiedSuccess(syncEntityType),
            findDateModifiedDeleted(syncEntityType),
            findDateModifiedFailed(syncEntityType)
        )
        val maxDateModified = dateModifiedOccurrencesInitial.maxByOrNull { it.dateModified }?.dateModified
        val dateModifiedOccurrences = dateModifiedOccurrencesInitial.filter { it.dateModified == maxDateModified }

        return if (dateModifiedOccurrences.isNotEmpty()) {
            val distinctUuids = dateModifiedOccurrences.flatMap { it.uuids }.distinct()
            val dateModified = dateModifiedOccurrences.first().dateModified
            DateModifiedOccurrence(uuids = distinctUuids, dateModified = dateModified)
        } else
            null
    }
}