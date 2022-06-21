package com.jnj.vaccinetracker.sync.domain.usecases.operator.base

import com.jnj.vaccinetracker.common.data.database.repositories.OperatorCredentialsRepository
import com.jnj.vaccinetracker.common.domain.entities.OperatorCredentials
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.yield

abstract class RemoveOperatorCredentialsUseCaseBase {

    protected abstract val operatorCredentialsRepository: OperatorCredentialsRepository


    protected suspend fun findOperators() = operatorCredentialsRepository.findAll()

    protected suspend fun OperatorCredentials.tryDelete(): Boolean {
        val cachedOperator = this
        return try {
            operatorCredentialsRepository.delete(cachedOperator)
            true
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("error when trying to delete $cachedOperator", ex)
            false
        }
    }

}