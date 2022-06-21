package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.debugLabel
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.helpers.ServerPollUtil
import com.jnj.vaccinetracker.sync.domain.usecases.operator.RemoveInvalidUsersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveUserSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val removeInvalidUsersUseCase: RemoveInvalidUsersUseCase,
    private val serverPollUtil: ServerPollUtil,
) {
    companion object {
        private val counter = Counters.ActiveUsersSync
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
        removeInvalidUsersUseCase.removeInvalidUsers()
    }


    private suspend fun pollServerPeriodically() {
        serverPollUtil.pollServerPeriodically(counter.DELAY, debugLabel()) { pollServer(); true }
    }

}