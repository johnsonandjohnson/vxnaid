package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.base.UploadableDraftRepository
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FindDraftParticipantUuidsPendingUploadUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val dispatchers: AppCoroutineDispatchers,
) {
    companion object {
        private const val LIMIT = 10
        private const val TARGET_SIZE = LIMIT
    }

    private fun UploadableDraftRepository<*>.calcLimit(): Int {
        return when (this) {
            is DraftVisitRepository -> LIMIT
            is DraftParticipantRepository -> LIMIT
            is DraftVisitEncounterRepository -> LIMIT
            else -> error("unsupported repository: $this")
        }
    }

    private fun calcOffset(limit: Int, page: Int): Int = (page - 1) * limit

    private suspend fun UploadableDraftRepository<*>.findAllParticipantUuidsPendingUploadByDraftState(page: Int): List<String> {
        val limit = calcLimit()
        return findAllParticipantUuidsByDraftState(
            draftState = DraftState.UPLOAD_PENDING,
            offset = calcOffset(
                limit = limit, page = page
            ),
            limit = limit
        )
    }

    private suspend fun findParticipantUuidsPendingUpload(page: Int): Collection<String> {
        require(page > 0) { "page must be greater than zero" }
        val linkedSet = mutableSetOf<String>()
        linkedSet += draftParticipantRepository.findAllParticipantUuidsPendingUploadByDraftState(page = page)
        linkedSet += draftVisitRepository.findAllParticipantUuidsPendingUploadByDraftState(page = page)
        linkedSet += draftVisitEncounterRepository.findAllParticipantUuidsPendingUploadByDraftState(page = page)
        return linkedSet
    }

    private suspend fun findParticipantUuidsPendingUpload(participantUuids: Collection<String>, skipParticipantUuidMap: Map<String, Boolean>, page: Int): Collection<String> {
        logInfo("findParticipantUuidsPendingUpload ${participantUuids.size} p$page")
        if (participantUuids.size >= TARGET_SIZE) {
            logInfo("target size reached, returning participantUuids with size ${participantUuids.size} p$page")
            return participantUuids
        }

        logInfo("findParticipantUuidsPendingUpload p$page ${participantUuids.size}/$TARGET_SIZE")
        val ids = findParticipantUuidsPendingUpload(page)
        if (ids.isEmpty()) {
            logInfo("ids empty returning participantUuids with size ${participantUuids.size} p$page")
            return participantUuids
        }

        return ids
            .filter { !(skipParticipantUuidMap[it] ?: false) }
            .let { results ->
                logInfo("findParticipantUuidsPendingUpload results ${results.size}")
                findParticipantUuidsPendingUpload(participantUuids + results, skipParticipantUuidMap, page + 1)
            }
    }

    suspend fun findParticipantUuidsPendingUpload(skipParticipantUuidMap: Map<String, Boolean>): List<String> = withContext(dispatchers.computation) {
        findParticipantUuidsPendingUpload(emptyList(), skipParticipantUuidMap, 1).toList()
    }
}