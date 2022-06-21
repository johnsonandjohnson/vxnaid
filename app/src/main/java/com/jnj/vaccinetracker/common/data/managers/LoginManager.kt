package com.jnj.vaccinetracker.common.data.managers

import com.jnj.vaccinetracker.common.domain.entities.User
import com.jnj.vaccinetracker.common.domain.usecases.OperatorLoginUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class LoginManager @Inject constructor(private val operatorLoginUseCase: OperatorLoginUseCase) {

    suspend fun login(username: String, password: String): User = operatorLoginUseCase.login(username, password)

}