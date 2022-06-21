package com.jnj.vaccinetracker.fake.data.network

import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.milliseconds
import com.jnj.vaccinetracker.common.helpers.minutes
import com.jnj.vaccinetracker.fake.data.random.RandomBiometricsTemplateGenerator
import com.jnj.vaccinetracker.fake.data.random.RandomImageGenerator
import com.jnj.vaccinetracker.fake.data.random.RandomParticipantGenerator
import com.jnj.vaccinetracker.fake.data.random.RandomVisitsGenerator
import com.jnj.vaccinetracker.sync.data.models.*
import com.soywiz.klock.DateTime
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeBackendDatabase @Inject constructor(private val fakeEngine: FakeEngine) {

    private suspend fun <T : SyncRecordBase> getRecords(syncRequest: SyncRequest, completeSyncRecordMapper: CompleteSyncRecord.() -> List<T>): SyncResponse<T> {
        val dateModifiedOffset = syncRequest.dateModifiedOffset
        val cachedRecords = fakeEngine.records(dateModifiedOffset, syncRequest.limit)
        logInfo("getRecords: $dateModifiedOffset ${cachedRecords.size}")
        val entities = cachedRecords
            .map { completeSyncRecord -> completeSyncRecordMapper(completeSyncRecord) }.flatten()
        val filteredEntities = if (dateModifiedOffset == null)
            entities
        else {
            entities.filter { it.dateModified >= dateModifiedOffset }
        }
        val records = filteredEntities.drop(syncRequest.offset).take(syncRequest.limit)
        val syncStatus = if (records.isEmpty()) SyncStatus.OK else SyncStatus.OUT_OF_SYNC
        return SyncResponse(dateModifiedOffset = dateModifiedOffset,
            syncScope = syncRequest.syncScope,
            uuidsWithDateModifiedOffset = syncRequest.uuidsWithDateModifiedOffset,
            limit = syncRequest.limit,
            syncStatus = syncStatus,
            totalSyncScopeRecordCount = null,
            totalIgnoredRecordCount = null,
            records = records)
    }

    suspend fun getAllParticipants(syncRequest: SyncRequest): ParticipantSyncResponse {
        return getRecords(syncRequest) {
            listOf(participant)
        }
    }

    suspend fun getAllParticipantBiometricsTemplates(syncRequest: SyncRequest): ParticipantBiometricsTemplateSyncResponse {
        return getRecords(syncRequest) {
            listOf(template)
        }
    }

    suspend fun getAllParticipantImages(syncRequest: SyncRequest): ParticipantImageSyncResponse {
        return getRecords(syncRequest) {
            listOf(image)
        }
    }

    suspend fun getAllVisits(syncRequest: SyncRequest): VisitSyncResponse {
        return getRecords(syncRequest) {
            visits
        }
    }
}

@Singleton
class FakeEngineSettings @Inject constructor(prefs: FlowSharedPreferences) {
    private val targetParticipantCount = prefs.getInt("targetParticipantCount")
    fun observeTargetParticipantCount(): Flow<Int> = targetParticipantCount.asFlow()
    fun getTargetParticipantCount(): Int {
        return targetParticipantCount.get()
    }

    fun setTargetParticipantCount(count: Int) {
        targetParticipantCount.set(count)
    }

    fun observeChanges(): Flow<Unit> = observeTargetParticipantCount().map { Unit }
}

data class CompleteSyncRecord(
    val participant: ParticipantSyncRecord,
    val visits: List<VisitSyncRecord>,
    val image: ParticipantImageSyncRecord,
    val template: ParticipantBiometricsTemplateSyncRecord,
    val expiresAt: Long,
) {
    val dateModified = visits.maxByOrNull { it.dateModified }?.dateModified ?: participant.dateModified

    fun isExpired() = System.currentTimeMillis() > expiresAt
}

class FakeEngine @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val participantGenerator: RandomParticipantGenerator,
    private val visitsGenerator: RandomVisitsGenerator,
    private val templateGenerator: RandomBiometricsTemplateGenerator,
    private val imageGenerator: RandomImageGenerator,
    private val fakeEngineSettings: FakeEngineSettings,
    private val participantRepository: ParticipantRepository,
) {
    private val mutex = Mutex()
    private var completeRecordsCache: List<CompleteSyncRecord> = emptyList()

    companion object {
        const val INITIAL_YEAR = 2021
    }

    private suspend fun generateNew(count: Int, defaultStartDate: SyncDate): Int {
        require(count >= 0)
        val targetCount = fakeEngineSettings.getTargetParticipantCount()
        logInfo("generateNew: $count $targetCount")
        if (count == 0)
            return 0

        if (targetCount <= 0)
            return 0

        val currentParticipantsCount = participantRepository.count()

        if (currentParticipantsCount >= targetCount) {
            return 0
        } else {
            logInfo("generating one participant data: total stored $currentParticipantsCount")
            val time = dateNow().time
            val lastDateModified = completeRecordsCache.lastOrNull()?.dateModified ?: defaultStartDate
            val dateModified = lastDateModified + 1.milliseconds
            val participant = participantGenerator.generateParticipant(dateModified = dateModified)
            val participantUuid = participant.participantUuid
            val template = templateGenerator.generateTemplate(participantUuid, dateModified)
            val image = imageGenerator.generateImage(participantUuid, dateModified)
            val visits = visitsGenerator.generateVisits(participant)
            val completeRecord = CompleteSyncRecord(participant, visits, image, template, System.currentTimeMillis() + 1.minutes)
            val timeElapsed = dateNow().time - time
            logInfo("generated a participant and related data in $timeElapsed ms")
            completeRecordsCache = completeRecordsCache + completeRecord
            return 1 + generateNew(count - 1, defaultStartDate)
        }
    }

    private fun cleanStale() {
        completeRecordsCache = completeRecordsCache.filter { !it.isExpired() }
    }

    private suspend fun generate(dateModifiedOffset: SyncDate?, limit: Int): Unit = withContext(dispatchers.io) {
        mutex.withLock {
            cleanStale()
            val minReserve = 5
            val generateCount = if (dateModifiedOffset != null) {
                val recordsAvailable = completeRecordsCache.count { it.dateModified >= dateModifiedOffset }
                (limit - recordsAvailable).coerceAtLeast(minReserve)
            } else (limit - completeRecordsCache.size).coerceAtLeast(minReserve)
            val defaultStartDate = SyncDate(DateTime(INITIAL_YEAR, 1, 1).unixMillisLong)
            val startDate = if (completeRecordsCache.isEmpty() && dateModifiedOffset != null) dateModifiedOffset else defaultStartDate
            val amountGenerated = generateNew(generateCount, startDate)
            if (amountGenerated == 0 && generateCount > 0)
                completeRecordsCache = emptyList()
        }
    }

    suspend fun records(dateModifiedOffset: SyncDate?, limit: Int): List<CompleteSyncRecord> {
        generate(dateModifiedOffset, limit)
        return completeRecordsCache.toList()
    }

}