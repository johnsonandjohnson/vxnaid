package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.base.BiometricsTemplateRepositoryCommon
import com.jnj.vaccinetracker.common.data.database.repositories.base.ParticipantRepositoryBase
import com.jnj.vaccinetracker.common.data.models.Constants.MAX_PERCENT
import com.jnj.vaccinetracker.common.data.models.toDomain
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.p2p.data.helpers.calcProgress
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class MatchParticipantsUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantRepository: ParticipantRepository,
    draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    private val biometricsMatcherUseCase: BiometricsMatcherUseCase,
    private val dispatchers: AppCoroutineDispatchers,
    private val getSelectedSiteUseCase: GetSelectedSiteUseCase,
    private val configurationUseCase: GetConfigurationUseCase,
) {
    companion object {
        private val TWO_WAY_AUTHENTICATION_REMOTE_READ_TIMEOUT = 6.seconds
    }

    suspend fun matchParticipants(identificationCriteria: ParticipantIdentificationCriteria, onProgressChanged: OnProgressPercentChanged = {}): List<ParticipantMatch> =
        withContext(dispatchers.io) {
            fun List<ParticipantMatch>.presentable() = sortedWith(ParticipantMatchComparator(identificationCriteria))
            var progressRemote = 0 // goes from 0 straight to 100
            var progressLocal = 0 // goes to 100
            fun setProgressState(changeBlock: () -> Unit) {
                changeBlock()
                onProgressChanged(combineProgressPercent(progressRemote.progress(), progressLocal.progress()))
            }
            setProgressState { }
            val matchesRemoteTask = async(dispatchers.io) {
                kotlin.runCatching {
                    fetchMatchesRemote(identificationCriteria)
                }.also {
                    setProgressState {
                        progressRemote = MAX_PERCENT
                    }
                }
            }
            val matchesLocalTask = async(dispatchers.computation) {
                kotlin.runCatching {
                    fetchMatchesLocal(identificationCriteria) { progressPercent ->
                        setProgressState {
                            progressLocal = progressPercent
                        }
                    }
                }.also {
                    setProgressState {
                        progressLocal = MAX_PERCENT
                    }
                }
            }
            val matchesRemote: List<ParticipantMatch>? = try {
                if (identificationCriteria.isTemplateOnly) {
                    matchesRemoteTask.await().getOrThrow()
                } else {
                    timeoutAfter(TWO_WAY_AUTHENTICATION_REMOTE_READ_TIMEOUT, isCancelTimeout = {
                        // cancel task only after timeout elapsed, local task has completed and failed.
                        matchesLocalTask.await().isFailure
                    }) {
                        matchesRemoteTask.await().getOrThrow()
                    }
                }
            } catch (ex: NoNetworkException) {
                logInfo("MatchParticipants no network")
                null
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("error with fetchMatchesRemote", ex)
                null
            }
            val matchesLocal = try {
                matchesLocalTask.await().getOrThrow()
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("failed to match local", ex)
                if (matchesRemote != null) {
                    return@withContext matchesRemote.presentable()
                } else {
                    throw ex
                }
            }
            logInfo("matchesRemote: ${matchesRemote?.size}, matchesLocal: ${matchesLocal.size}")
            matchesRemote?.forEachIndexed { index, participantMatch ->
                logInfo("remote match #$index: ${participantMatch.participantId} ${participantMatch.uuid} @ ${participantMatch.locationUuid}")
            }
            matchesLocal.forEachIndexed { index, participantMatch ->
                logInfo("local match #$index: ${participantMatch.participantId} ${participantMatch.uuid} @ ${participantMatch.locationUuid}")
            }
            (matchesRemote.orEmpty() + matchesLocal)
                .distinctBy { it.uuid }
                .presentable()
        }

    private val participantRepos = listOf<ParticipantRepositoryBase<out ParticipantBase>>(participantRepository, draftParticipantRepository)
    private val templateRepos =
        listOf<BiometricsTemplateRepositoryCommon<*>>(participantBiometricsTemplateRepository, draftParticipantBiometricsTemplateRepository)

    private suspend fun fetchMatchesRemote(identificationCriteria: ParticipantIdentificationCriteria): List<ParticipantMatch> {
        logInfo("fetchMatchesRemote")
        return api.matchParticipants(
            participantId = identificationCriteria.participantId,
            phone = identificationCriteria.phone,
            biometricsTemplateFile = identificationCriteria.biometricsTemplate,
            country = getSelectedSiteUseCase.getSelectedSite().country
        ).map { it.toDomain() }
    }

    private suspend fun ParticipantBiometricsTemplateFileBase.findParticipant(): ParticipantBase? {
        return when (this) {
            is DraftParticipantBiometricsTemplateFile -> draftParticipantRepository.findByParticipantUuid(participantUuid)
                ?: participantRepository.findByParticipantUuid(participantUuid)
            is ParticipantBiometricsTemplateFile -> participantRepository.findByParticipantUuid(participantUuid)
                ?: draftParticipantRepository.findByParticipantUuid(participantUuid)
        }
    }

    private suspend fun List<BiometricsFileMatch>.toLocalParticipantMatches(participants: List<ParticipantBase>) =
        mapNotNull { biometricsMatch ->
            val participant = participants.find { it.participantUuid == biometricsMatch.uuid } ?: biometricsMatch.template.findParticipant()
            if (participant != null)
                LocalParticipantMatch(participant, biometricsMatch.matchingScore)
            else {
                logError("couldn't find participant for biometricsMatch {}", biometricsMatch)
                null
            }
        }

    private suspend fun findParticipants(participantId: String?, phone: String?): List<ParticipantBase> {
        val results = mutableListOf<ParticipantBase>()
        if (!participantId.isNullOrEmpty()) {
            for (repo in participantRepos) {
                val participant = repo.findByParticipantId(participantId)
                logInfo("participant id match (participantId:$participantId) ${repo::class.simpleName}: ${participant?.participantId}")
                if (participant != null) {
                    results += participant
                    break
                }
            }
        }
        if (!phone.isNullOrEmpty()) {
            for (repo in participantRepos) {
                val participants = repo.findAllByPhone(phone)
                logInfo("participant phone match (phone:$phone) ${repo::class.simpleName}: ${participants.size}")
                results += participants
            }
        }
        return results.distinctBy { it.participantUuid }
    }

    private suspend fun fetchMatchesWithoutBiometrics(
        criteria: ParticipantIdentificationCriteria,
        onProgressPercentChanged: OnProgressPercentChanged,
    ): List<LocalParticipantMatch> {
        logInfo("fetchMatchesWithoutBiometrics")
        return findParticipants(participantId = criteria.participantId, phone = criteria.phone).map { it.toLocalMatch() }.also {
            onProgressPercentChanged(MAX_PERCENT)
        }
    }

    private suspend fun fetchMatchesWithBiometrics(
        criteria: ParticipantIdentificationCriteria,
        onProgressPercentChanged: OnProgressPercentChanged,
    ): List<LocalParticipantMatch> {
        requireNotNull(criteria.biometricsTemplate) { "template is required for fetchMatchesWithBiometrics" }
        logInfo("fetchMatchesWithBiometrics")
        val templatesToEnroll = mutableListOf<ParticipantBiometricsTemplateFileBase>()
        var progressLoadParticipants = 0
        var progressLoadTemplates = 0
        var progressMatcher = 0
        var progressFetchMatchParticipants = 0
        fun setProgressState(changeBlock: () -> Unit) {
            changeBlock()
            onProgressPercentChanged(combineProgressPercent(
                progressLoadParticipants.progress(5),
                progressLoadTemplates.progress(20),
                progressMatcher.progress(70),
                progressFetchMatchParticipants.progress(5),
            ))
        }

        val participantsByPhoneOrId = findParticipants(criteria.participantId, criteria.phone)
        participantsByPhoneOrId.mapNotNull { it.biometricsTemplate }.forEach { template ->
            templatesToEnroll += template
        }
        setProgressState {
            progressLoadParticipants = MAX_PERCENT
        }
        if (criteria.isTemplateOnly) {
            var i = 1
            for (repo in templateRepos) {
                val templates = repo.findAll()

                logInfo("included templates ${repo::class.simpleName}: ${templates.size}")
                templatesToEnroll += templates
                setProgressState {
                    progressLoadTemplates = calcProgress(i, templates.size)
                }
                i++
            }
        } else {
            setProgressState {
                progressLoadTemplates = MAX_PERCENT
            }
        }
        val uniqueTemplatesToEnroll = templatesToEnroll.distinctBy { it.participantUuid }
        val matches = biometricsMatcherUseCase.match(uniqueTemplatesToEnroll, criteria.biometricsTemplate) { progressPercent ->
            setProgressState {
                progressMatcher = progressPercent
            }
        }
        setProgressState {
            progressMatcher = MAX_PERCENT
        }
        logInfo("fetchMatchesWithBiometrics matches count ${matches.size} templatesToEnroll size = ${templatesToEnroll.size}")
        val matchesCombined = matches
            .toLocalParticipantMatches(participantsByPhoneOrId) +
                participantsByPhoneOrId.map { it.toLocalMatch() }
        return matchesCombined.distinctBy { it.uuid }.also {
            setProgressState {
                progressFetchMatchParticipants = MAX_PERCENT
            }
        }
    }

    private suspend fun fetchMatchesLocal(criteria: ParticipantIdentificationCriteria, onProgressPercentChanged: OnProgressPercentChanged): List<ParticipantMatch> {
        val localMatches = if (criteria.biometricsTemplate == null) {
            fetchMatchesWithoutBiometrics(
                criteria,
                onProgressPercentChanged = onProgressPercentChanged
            )
        } else {
            fetchMatchesWithBiometrics(
                criteria,
                onProgressPercentChanged = onProgressPercentChanged
            )
        }
        return localMatches.map { it.toDomain() }
    }

    private fun LocalParticipantMatch.toDomain() = with(participant) {
        ParticipantMatch(
            participantUuid,
            participantId = participantId,
            matchingScore = matchingScore,
            gender = gender,
            birthDate = birthDate,
            address = address,
            attributes = attributes,
        )
    }

    private fun ParticipantBase.toLocalMatch(matchingScore: Int? = null) = LocalParticipantMatch(this, matchingScore)
}
