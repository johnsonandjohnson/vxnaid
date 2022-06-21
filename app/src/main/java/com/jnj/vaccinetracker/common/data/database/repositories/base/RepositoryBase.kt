package com.jnj.vaccinetracker.common.data.database.repositories.base

import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFileBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface RepositoryBase<in T> {
    suspend fun insert(model: T, orReplace: Boolean)
    fun observeChanges(): Flow<Long>
    suspend fun count(): Long = observeChanges().first()
}

interface SyncRepositoryBase {
    suspend fun findMostRecentDateModifiedOccurrence(): DateModifiedOccurrence?
    suspend fun deleteAll()
}

interface DeleteByDraftParticipant {
    suspend fun deleteByParticipantUuid(participantUuid: String): Boolean
}

/**
 * repositories for entities with a [DraftState]
 */
interface UpdatableDraftRepository<T> {
    suspend fun updateDraftState(draft: T)
    fun observeChanges(): Flow<*>
    suspend fun deleteAllUploaded()
    suspend fun countByDraftState(draftState: DraftState): Long
}

/**
 * repositories for entities with a [DraftState] and a REST call equivalent
 */
interface UploadableDraftRepository<T> : UpdatableDraftRepository<T> {
    suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<String>
}


interface ParticipantRepositoryBase<T> : RepositoryBase<T> {
    suspend fun findByParticipantUuid(participantUuid: String): T?
    suspend fun findByParticipantId(participantId: String): T?
    suspend fun findAllByPhone(phone: String?): List<T>
}

interface ParticipantRepositoryCommon<T> : ParticipantRepositoryBase<T> {
    suspend fun findRegimen(participantUuid: String): String?
}

interface DraftParticipantRepositoryBase<T> : ParticipantRepositoryCommon<T>, UploadableDraftRepository<T> {
    suspend fun findByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): T?
}

interface ParticipantDataFileRepositoryCommon<T> : RepositoryBase<T> {
    suspend fun forEachAll(onPageResult: suspend (List<T>) -> Unit)
}

interface ParticipantDataFileRepositoryBase<T> : ParticipantDataFileRepositoryCommon<T> {
    suspend fun findByParticipantUuid(participantUuid: String): T?
}

interface DraftParticipantDataFileRepositoryBase<T> : ParticipantDataFileRepositoryCommon<T>, UpdatableDraftRepository<T> {
    suspend fun findByParticipantUuid(participantUuid: String): T?
    suspend fun findAllUploaded(offset: Int, limit: Int): List<T>
}

interface VisitRepositoryBase<T> : RepositoryBase<T> {
    suspend fun findByVisitUuid(visitUuid: String): T?
    suspend fun findAllByParticipantUuid(participantUuid: String): List<T>
    suspend fun deleteByVisitUuid(visitUuid: String): Boolean
}


interface DraftVisitRepositoryBase<T> : VisitRepositoryBase<T>, UploadableDraftRepository<T> {
    suspend fun findAllByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): List<T>
    suspend fun findDraftStateByVisitUuid(visitUuid: String): DraftState?
    suspend fun deleteByVisitUuid(visitUuid: String, draftState: DraftState): Boolean
}

interface BiometricsTemplateRepositoryCommon<T : ParticipantBiometricsTemplateFileBase> : ParticipantRepositoryBase<T> {
    suspend fun findAll(): List<T>
}

interface BiometricsTemplateRepositoryBase<T : ParticipantBiometricsTemplateFileBase> : BiometricsTemplateRepositoryCommon<T>, ParticipantDataFileRepositoryBase<T>

interface DraftBiometricsTemplateRepositoryBase<T : ParticipantBiometricsTemplateFileBase> : BiometricsTemplateRepositoryCommon<T>, DraftParticipantDataFileRepositoryBase<T>