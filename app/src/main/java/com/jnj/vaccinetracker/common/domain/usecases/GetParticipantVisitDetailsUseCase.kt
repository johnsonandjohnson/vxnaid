package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.models.api.response.toDomain
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetParticipantVisitDetailsUseCase @Inject constructor(
    private val draftVisitRepository: DraftVisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val visitRepository: VisitRepository,
    private val api: VaccineTrackerSyncApiDataSource,
    private val dispatchers: AppCoroutineDispatchers,
) {
    companion object {
        private val REMOTE_READ_TIMEOUT = 6.seconds
    }

    /**
     * fetch visits for participant by [participantUuid] from local + remote
     * @param participantUuid the identifier of a participant
     */
    suspend fun getParticipantVisitDetails(participantUuid: String): List<VisitDetail> = withContext(dispatchers.io) {
        logInfo("getParticipantVisitDetails: $participantUuid")
        val visitsRemoteTask = async(dispatchers.io) {
            kotlin.runCatching {
                fetchVisitDetailsRemote(participantUuid)
            }
        }
        val visitsLocalTask = async(dispatchers.computation) {
            kotlin.runCatching {
                readFromDatabase(participantUuid)
            }

        }
        // we care about remote visits in case sync is not completed or there's an issue with sync
        val visitsRemote = try {
            timeoutAfter(REMOTE_READ_TIMEOUT, isCancelTimeout = {
                visitsLocalTask.await().isFailure
            }) {
                visitsRemoteTask.await().getOrThrow()
            }
        } catch (ex: NoNetworkException) {
            logInfo("getParticipantVisitDetailsRemote no network")
            null
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("error with getParticipantVisitDetailsRemote", ex)
            null
        }
        val visitsLocal = try {
            visitsLocalTask.await().getOrThrow()
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("failed to match local", ex)
            if (visitsRemote != null) {
                return@withContext visitsRemote
            } else {
                throw ex
            }
        }
        logInfo("getParticipantVisitDetails success local:${visitsLocal.size}, remote:${visitsRemote?.size}")
        visitsRemote.orEmpty().mergeWith(visitsLocal)
    }

    private fun Visit.mergeWith(encounter: DraftVisitEncounter?): VisitDetail {
        val visitDetail = this.toVisitDetail()
        return if (encounter != null && visitDetail.observations.isEmpty()) {
            visitDetail.mergeWith(encounter)
        } else visitDetail
    }

    private fun VisitDetail.mergeWith(encounter: DraftVisitEncounter): VisitDetail {
        return copy(attributes = this.attributes + encounter.attributes,
            visitDate = encounter.startDatetime,
            observations = encounter.observationsWithDate)
    }

    private fun DraftVisitEncounter.toVisitDetail(): VisitDetail {
        return VisitDetail(
            uuid = visitUuid,
            visitType = visitType,
            visitDate = startDatetime,
            attributes = attributes,
            observations = observationsWithDate)
    }


    private fun Visit.toVisitDetail(): VisitDetail {
        return VisitDetail(
            uuid = visitUuid,
            visitType = visitType,
            visitDate = startDatetime,
            attributes = attributes,
            observations = observations)
    }

    private fun DraftVisit.toVisitDetail(): VisitDetail {
        return VisitDetail(
            uuid = visitUuid,
            visitType = visitType,
            visitDate = startDatetime,
            attributes = attributes,
            observations = emptyMap())
    }

    private fun DraftVisit.mergeWith(encounter: DraftVisitEncounter?): VisitDetail {
        val visitDetail = toVisitDetail()
        return if (encounter != null)
            visitDetail.mergeWith(encounter)
        else
            visitDetail
    }

    private suspend fun readFromDatabase(participantUuid: String): List<VisitDetail> {
        val serverVisitMap = visitRepository.findAllByParticipantUuid(participantUuid).associateBy { it.visitUuid }
        val draftVisitMap = draftVisitRepository.findAllByParticipantUuid(participantUuid).associateBy { it.visitUuid }
        val draftEncounterMap = draftVisitEncounterRepository.findAllByParticipantUuid(participantUuid).associateBy { it.visitUuid }
        val allKeys = (serverVisitMap.keys + draftVisitMap.keys + draftEncounterMap.keys).distinct()
        return allKeys.map { visitUuid ->
            val visit = serverVisitMap[visitUuid]
            val draftVisit = draftVisitMap[visitUuid]
            val draftEncounter = draftEncounterMap[visitUuid]
            when {
                visit != null -> visit.mergeWith(draftEncounter)
                draftVisit != null -> draftVisit.mergeWith(draftEncounter)
                draftEncounter != null -> {
                    // this can happen if visits have been deleted or encounter was logged for remote visit that hasn't synced yet
                    logWarn("visit and draft visit is null: {}", draftEncounter)
                    draftEncounter.toVisitDetail()
                }
                else -> {
                    // this will never occur, just making compiler happy
                    error("visit, draftVisit and draftEncounter are null")
                }
            }
        }.sortedBy { it.startDate }
    }

    private suspend fun fetchVisitDetailsRemote(participantUuid: String): List<VisitDetail> {
        return api.getParticipantVisitDetails(participantUuid).map { it.toDomain() }
    }


    private fun VisitDetail.mergeWith(otherVisit: VisitDetail): VisitDetail {
        fun VisitDetail.encTime() = encounterDate?.time ?: 0
        // prefer most recent encounter date
        return if (encTime() >= otherVisit.encTime())
            this
        else
            otherVisit
    }

    private fun List<VisitDetail>.mergeWith(otherVisits: List<VisitDetail>): List<VisitDetail> {
        val mapA = associateBy { it.uuid }
        val mapB = otherVisits.associateBy { it.uuid }
        return (mapA.keys + mapB.keys).distinct().map { key ->
            val visitA = mapA[key]
            val visitB = mapB[key]
            when (visitA) {
                null -> requireNotNull(visitB) { "visitB must not be null" }
                else -> when (visitB) {
                    null -> visitA
                    else -> visitA.mergeWith(visitB)
                }
            }
        }
    }
}