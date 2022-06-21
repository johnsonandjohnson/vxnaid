package com.jnj.vaccinetracker.sync.domain.usecases.operator

import com.jnj.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.jnj.vaccinetracker.common.domain.entities.OperatorCredentials
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.usecases.operator.base.RemoveOperatorCredentialsUseCaseBase
import javax.inject.Inject

class RemoveInvalidUsersUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val operatorCredentialsRepository: OperatorCredentialsRepository,
) : RemoveOperatorCredentialsUseCaseBase() {

    private suspend fun List<OperatorCredentials>.deleteInactiveUsers(): List<OperatorCredentials> {
        val activeUsers = api.getActiveUsers()
        return filter { cachedOperator ->
            val isValid = activeUsers.any { it.uuid == cachedOperator.uuid }
            val isDeleted = !isValid && cachedOperator.tryDelete()
            !isDeleted
        }
    }

    suspend fun removeInvalidUsers() {
        val cachedCredentials = findOperators()
        if (cachedCredentials.isEmpty()) {
            logInfo("removeInvalidUsers: cached credentials empty")
            return
        }
        val usersLeft = cachedCredentials.deleteInactiveUsers()
        logInfo("removeInvalidUsers: ${cachedCredentials.size} -> ${usersLeft.size}")
    }
}