package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.domain.usecases.FindMostRecentDateModifiedOccurrenceUseCase
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.helpers.ServerPollUtil
import com.jnj.vaccinetracker.sync.data.mappers.SyncScopeToDtoMapper
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.models.SyncRequest
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel
import com.jnj.vaccinetracker.sync.data.models.SyncStatus
import com.jnj.vaccinetracker.sync.domain.entities.SyncPageProgress
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import com.jnj.vaccinetracker.sync.domain.factories.DownloadSyncRecordsUseCaseFactory
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.usecases.failed.DownloadFailedSyncRecordsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.FindAllFailedSyncRecordsByDateLastDownloadAttemptUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.syncscope.GetSyncScopeUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParticipantDataDownstreamSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val networkConnectivity: NetworkConnectivity,
    private val findMostRecentDateModifiedOccurrenceUseCase: FindMostRecentDateModifiedOccurrenceUseCase,
    private val getSyncScopeUseCase: GetSyncScopeUseCase,
    private val downloadSyncRecordsUseCaseFactory: DownloadSyncRecordsUseCaseFactory,
    private val syncSettingsObserver: SyncSettingsObserver,
    private val serverPollUtil: ServerPollUtil,
    private val syncLogger: SyncLogger,
    private val downloadFailedSyncRecordsUseCase: DownloadFailedSyncRecordsUseCase,
    private val findAllFailedSyncRecordsByDateLastDownloadAttemptUseCase: FindAllFailedSyncRecordsByDateLastDownloadAttemptUseCase,
    private val syncScopeToDtoMapper: SyncScopeToDtoMapper,
) {
    companion object {
        private val counter = Counters.DownstreamSync

        private val SyncEntityType.optimizedDefault: Boolean
            get() = when (this) {
                SyncEntityType.PARTICIPANT -> false
                SyncEntityType.IMAGE, SyncEntityType.BIOMETRICS_TEMPLATE -> true
                SyncEntityType.VISIT -> false
            }

        fun SyncEntityType.shouldOptimize(
            syncScope: SyncScope,
            dateModifiedOccurrence: DateModifiedOccurrence?,
            safetyDateOffset: Long = counter.SHOULD_OPTIMIZE_DATE_OFFSET,
        ): Boolean {
            return if (dateModifiedOccurrence != null) {
                // when sync scope is changed, only optimize bandwidth for future records
                val syncScopeDate = syncScope.dateCreated.time
                val lastDateModifiedSync = dateModifiedOccurrence.dateModified.time
                // in case a timezone date is misinterpreted as UTC date, add an offset to be safe
                optimizedDefault && (lastDateModifiedSync - safetyDateOffset) > syncScopeDate
            } else {
                // don't optimize initial page
                false
            }
        }
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var pollServerJob: Job? = null

    private val pollSignal = MutableSharedFlow<Int>(0, 10)


    private val syncResults: SyncResults = mutableMapOf()
    private var syncCycle: Int = 0
    private val syncInProgressMap = mutableMapOf<SyncEntityType, Boolean>()

    fun start() {
        logInfo("start ${pollServerJob?.isActive}")
        if (pollServerJob?.isActive != true) {
            launchSyncCoroutines()
            pollServerJob = scope.launch {
                pollServerPeriodically()
            }

        }
    }

    private fun isAnyInProgress() = syncInProgressMap.values.any { inProgress -> inProgress }

    private fun logSyncInProgress(syncEntityType: SyncEntityType, inProgress: Boolean) {
        syncInProgressMap[syncEntityType] = inProgress
        syncLogger.logParticipantDownloadInProgress(isAnyInProgress())
    }

    private fun launchSyncCoroutines() {
        SyncEntityType.values().onEach { syncEntityType ->
            pollSignal
                .conflate()
                .onEach { syncCycle ->
                    onEachSyncCycle(syncCycle, syncEntityType)
                }.launchIn(scope)
        }
    }

    private suspend fun onEachSyncCycle(syncCycle: Int, syncEntityType: SyncEntityType) {
        logSyncInProgress(syncEntityType, true)
        try {
            val success = sync(syncEntityType, downloadFailed = true)
            onSyncFinished(syncCycle, syncEntityType, success)
        } finally {
            logSyncInProgress(syncEntityType, false)
        }
    }


    private val SyncEntityType.pageSize
        get() = when (this) {
            SyncEntityType.PARTICIPANT -> 100
            SyncEntityType.IMAGE -> 50
            SyncEntityType.BIOMETRICS_TEMPLATE -> 50
            SyncEntityType.VISIT -> 100
        }

    private suspend fun SyncEntityType.toSyncRequest(): SyncRequest {
        syncLogger.logSyncPageProgress(this, SyncPageProgress.BUILDING_SYNC_REQUEST)
        var syncScope = getSyncScopeUseCase.getSyncScope()
        if (this == SyncEntityType.IMAGE) {
            // we'll never need images of other sites
            syncScope = syncScope.copy(level = SyncScopeLevel.SITE)
        }
        val dateModifiedOccurrence = findMostRecentDateModifiedOccurrenceUseCase.findMostRecentDateModifiedOccurrence(this)
        logInfo("$this.toSyncRequest {}", dateModifiedOccurrence)
        val dateModifiedOffset = dateModifiedOccurrence?.dateModified?.let { SyncDate(it) }
        val syncScopeDto = syncScopeToDtoMapper.toDto(syncScope)
        val uuidsWithDateModifiedOffset = dateModifiedOccurrence?.uuids.orEmpty()
        val limit = this.pageSize
        val isOptimized = shouldOptimize(syncScope, dateModifiedOccurrence)
        return SyncRequest(
            dateModifiedOffset = dateModifiedOffset,
            syncScope = syncScopeDto,
            limit = limit,
            optimize = isOptimized,
            uuidsWithDateModifiedOffset = uuidsWithDateModifiedOffset
        )
    }

    private suspend fun downloadFailedSyncRecords(syncEntityType: SyncEntityType) {
        val date = SyncDate(dateNow().time - counter.DELAY_FAILED_RECORD_DOWNLOAD)
        val pageSize = syncEntityType.pageSize
        while (true) {
            val failedRecords = findAllFailedSyncRecordsByDateLastDownloadAttemptUseCase.findAllByDateLastDownloadAttemptLesserThan(syncEntityType, date, pageSize)
            if (failedRecords.isEmpty())
                break
            downloadFailedSyncRecordsUseCase.download(failedRecords)
        }
    }

    /**
     * @return success
     */
    private suspend fun downloadSyncRecords(syncEntityType: SyncEntityType): Boolean {
        val syncRequest = syncEntityType.toSyncRequest()
        val downloader = downloadSyncRecordsUseCaseFactory.create(syncEntityType)
        syncLogger.logSyncPageProgress(syncEntityType, SyncPageProgress.DOWNLOADING_PAGE)
        val syncStatus = try {
            downloader.download(syncRequest)
        } finally {
            syncLogger.logSyncPageProgress(syncEntityType, SyncPageProgress.IDLE)
        }
        return when (syncStatus) {
            SyncStatus.OUT_OF_SYNC -> sync(syncEntityType, downloadFailed = false)
            SyncStatus.OK -> {
                logInfo("sync ok [$syncEntityType]")
                true
            }
        }
    }

    /**
     * wait until participant uploading is done if it's in progress.
     */
    private suspend fun awaitUploadDone() {
        syncLogger.observeUploadInProgress()
            .filter { isUploading -> !isUploading }
            .await()
        syncLogger.observeFailedBiometricsTemplateUploadInProgress()
            .filter { isUploading -> !isUploading }
            .await()
    }

    /**
     * wait until master data sync finished if it's in progress.
     */
    private suspend fun awaitMasterDataSyncDone() {
        syncLogger.observeMasterDataSyncInProgress()
            .filter { isSyncingMasterData -> !isSyncingMasterData }
            .await()
    }

    /**
     * @return success
     */
    private suspend fun sync(syncEntityType: SyncEntityType, downloadFailed: Boolean): Boolean {
        logInfo("sync $syncEntityType")
        networkConnectivity.awaitFastInternet(debugLabel())
        syncSettingsObserver.awaitSyncSettingsAvailable(debugLabel())
        var success = false
        try {
            awaitUploadDone()
            awaitMasterDataSyncDone()
            if (downloadFailed) {
                downloadFailedSyncRecords(syncEntityType)
            }
            success = downloadSyncRecords(syncEntityType)
        } catch (ex: NoNetworkException) {
            sync(syncEntityType, downloadFailed = downloadFailed)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("error during sync $syncEntityType", ex)
            if (!networkConnectivity.isConnectedAccurate()) {
                logInfo("we are offline, trying again when online [$syncEntityType]")
                return sync(syncEntityType, downloadFailed = downloadFailed)
            }
            if (!syncSettingsObserver.isSyncSettingsAvailable()) {
                logInfo("sync settings not available, trying again when available [$syncEntityType]")
                return sync(syncEntityType, downloadFailed = downloadFailed)
            }
            if (ex is IOException) {
                logWarn("network available but network connectivity state might be incorrect, skipping this sync for now [$syncEntityType]")
            } else {
                logWarn("skipping sync due to unknown error [$syncEntityType]")
            }
        }

        return success
    }

    private fun createSyncDate() {
        require(syncCycle > 0)
        // is sync done of last sync cycle?
        val isSyncDone = syncResults[syncCycle]?.keys?.containsAll(SyncEntityType.values().toList()) ?: false
        if (isSyncDone) {
            val successfulEntityTypes = syncResults.findEntityTypesBySuccess(true)
            // it's possible sync failed for some sync entity types this sync cycle but they were successful in one of the last sync cycles since our last date recording
            // so when each sync entity type is successfully synced at least once since our last date recording then we'll clear the [syncResults] and record the date.
            if (successfulEntityTypes.containsAll(SyncEntityType.values().toList())) {
                val syncDate = syncResults.calcSyncDate()
                logInfo("createSyncDate - clearing sync results and logging sync date: $syncDate")
                syncResults.clear()
                syncLogger.logSyncCompletedDate(syncDate)
            } else {
                val unsuccessfulEntityTypes = syncResults.findEntityTypesBySuccess(false)
                logWarn("createSyncDate - not all successful entity types present succ:$successfulEntityTypes unsucc:$unsuccessfulEntityTypes")
            }
        } else {
            logWarn("createSyncDate - sync not done")
        }
    }


    private fun onSyncFinished(syncCycle: Int, syncEntityType: SyncEntityType, success: Boolean) {
        var map = syncResults[syncCycle] ?: emptyMap()
        map = map + (syncEntityType to SyncCompleted(success, SyncDate(dateNow())))
        syncResults[syncCycle] = map
        createSyncDate()
    }


    private suspend fun pollServerPeriodically() {
        logInfo("pollServerPeriodically syncCycle=0 empty syncResults")
        serverPollUtil.pollServerPeriodically(counter.DELAY_SERVER_POLL, "ParticipantDataDownstreamSyncService") {
            //at least one of the sync entity types has synced last sync cycle, otherwise they were waiting for internet for example
            //or it's empty
            if (syncResults.isEmpty() || syncResults.keys.maxOrNull() ?: 0 == syncCycle)
                pollSignal.tryEmit(++syncCycle)
            true
        }
    }

}

class SyncCompleted(val success: Boolean, val dateCompleted: SyncDate = SyncDate(dateNow()))
/**
 * map description: {sync cycle -> { sync entity type -> success,date } }
 */
typealias SyncResults = MutableMap<Int, Map<SyncEntityType, SyncCompleted>>

/**
 * which distinct [SyncEntityType] inside [SyncResults] have been successful
 */
fun SyncResults.findEntityTypesBySuccess(success: Boolean): List<SyncEntityType> =
    values.map { map -> map.entries.filter { it.value.success == success } }.flatten().map { it.key }.distinct()

/**
 * the oldest date when all sync entity types were synced
 */
fun SyncResults.calcSyncDate(): SyncDate {
    return values.asSequence().map { it.entries.map { (key, value) -> key to value } }.flatten()
        .filter { it.second.success }
        .sortedByDescending { it.second.dateCompleted }.distinctBy { it.first }.last().second.dateCompleted
}
