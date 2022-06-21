package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.debugLabel
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.helpers.ServerPollUtil
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.upload.UploadAllFailedBiometricsTemplatesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricsTemplateSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val uploadAllFailedBiometricsTemplatesUseCase: UploadAllFailedBiometricsTemplatesUseCase,
    private val serverPollUtil: ServerPollUtil,
    private val syncLogger: SyncLogger,
) {
    companion object {
        private val counter = Counters.FailedUploadBiometricTemplateSync
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var pollServerJob: Job? = null

    fun start() {
        if (pollServerJob?.isActive != true) {
            pollServerJob = scope.launch {
                pollServerPeriodically()
            }
        }
    }

    private suspend fun pollServer() {
        syncLogger.logFailedBiometricsTemplateUploadInProgress(true)
        try {
            uploadAllFailedBiometricsTemplatesUseCase.uploadFailedTemplates(timeSinceLastUploadAttempt = counter.TIME_SINCE_LAST_UPLOAD_ATTEMPT)
        } finally {
            syncLogger.logFailedBiometricsTemplateUploadInProgress(false)
        }
    }


    private suspend fun pollServerPeriodically() {
        serverPollUtil.pollServerPeriodically(counter.DELAY, debugLabel()) { pollServer(); true }
    }

}