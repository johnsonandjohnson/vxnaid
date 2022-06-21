package com.jnj.vaccinetracker.common.data.models.api.response

import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.User
import com.squareup.moshi.JsonClass

/**
 * @author maartenvangiel
 * @version 1
 */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    val authenticated: Boolean,
    val sessionId: String?,
    val user: UserDto?,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val uuid: String,
    val display: String,
    val username: String,
    val roles: List<UserRole>,
)

@JsonClass(generateAdapter = true)
data class UserRole(
    val uuid: String,
    val name: String,
)

fun List<UserRole>.isSyncAdmin() = any { it.name == Constants.ROLE_SYNC_ADMIN }
fun List<UserRole>.isOperator() = any { it.name == Constants.ROLE_OPERATOR }

fun UserDto.toDomain() = User(uuid = uuid, display = display, username = username)