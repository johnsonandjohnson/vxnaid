package com.jnj.vaccinetracker.sync.presentation

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.domain.helpers.SyncStateObserver
import com.jnj.vaccinetracker.sync.domain.services.*
import com.jnj.vaccinetracker.sync.presentation.base.ForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class SyncController @Inject constructor(
    private val syncNotificationManager: SyncNotificationManager,
    private val masterDataSyncService: MasterDataSyncService,
    private val participantPendingCallService: ParticipantPendingCallService,
    private val downstreamSyncService: ParticipantDataDownstreamSyncService,
    private val activeUserSyncService: ActiveUserSyncService,
    private val licenseSyncService: LicenseSyncService,
    private val dispatchers: AppCoroutineDispatchers,
    private val syncStateObserver: SyncStateObserver,
    private val syncStateService: SyncStateService,
    private val userExpirySyncService: UserExpirySyncService,
    private val syncCompletedDateSyncService: SyncCompletedDateSyncService,
    private val syncErrorSyncService: SyncErrorSyncService,
    private val heartBeatWakeUpService: HeartBeatWakeUpService,
    private val deviceNameSyncService: DeviceNameSyncService,
    private val biometricsTemplateSyncService: BiometricsTemplateSyncService,
    @Suppress("unused") private val buildTypeService: BuildTypeService,
) {

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    init {
        logInfo("init SyncController")
    }


    fun onCreate(foregroundService: ForegroundService) {
        logInfo("onCreate")
        syncNotificationManager.onCreate(foregroundService)
        licenseSyncService.start()
        masterDataSyncService.start()
        participantPendingCallService.startUploading()
        downstreamSyncService.start()
        activeUserSyncService.start()
        syncStateService.start()
        userExpirySyncService.start()
        syncCompletedDateSyncService.start()
        syncErrorSyncService.startUploadingErrors()
        observeSyncState(foregroundService)
        heartBeatWakeUpService.startWakeUpHeartBeat()
        deviceNameSyncService.start()
        biometricsTemplateSyncService.start()
    }

    private fun observeSyncState(foregroundService: ForegroundService) {
        syncStateObserver.observeSyncState().onEach { syncState ->
            syncNotificationManager.updateNotification(foregroundService, syncState)
        }.launchIn(scope)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onDestroy(foregroundService: ForegroundService) {
        logInfo("onDestroy")
        //don't cancel any singletons as the [foregroundService] will probably be restarted
        job.cancel()
        foregroundService.stopForeground(true)
    }
}