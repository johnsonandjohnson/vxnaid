package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.helpers.delaySafe
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.debugLabel
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import com.jnj.vaccinetracker.sync.domain.usecases.RemoveLoggedInTimedOutUserUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.operator.RemoveExpiredUsersUseCase
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserExpirySyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val removeExpiredUsersUseCase: RemoveExpiredUsersUseCase,
    private val removeLoggedInTimedOutUserUseCase: RemoveLoggedInTimedOutUserUseCase,
    private val syncSettingsObserver: SyncSettingsObserver,
) {
    companion object {
        private val counter = Counters.UserExpirySync
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var taskJob: Job? = null

    fun start() {
        if (taskJob?.isActive != true) {
            taskJob = scope.launch(dispatchers.io) {
                doTaskPeriodically()
            }
        }
    }

    private suspend fun doTask() {
        syncSettingsObserver.awaitNsdDisconnected(debugLabel())
        removeExpiredUsersUseCase.removeExpiredUsers()
        removeLoggedInTimedOutUserUseCase.removeLoggedInUserIfTimedOut()
    }


    private suspend fun doTaskPeriodically() {
        while (true) {
            try {
                doTask()
            } catch (ex: Throwable) {
                yield()
                ex.rethrowIfFatal()
                logError("doTaskPeriodically - something went wrong", ex)
            }
            delaySafe(counter.DELAY)
        }
    }

}