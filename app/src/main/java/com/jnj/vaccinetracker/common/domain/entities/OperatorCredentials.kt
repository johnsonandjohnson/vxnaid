package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow

data class OperatorCredentials(val uuid: String, val username: String, val display: String, val passwordHash: String, val dateCreated: DateEntity) {
    val user: User get() = User(uuid, display, username)

    companion object {
        fun fromUser(user: User, passwordHash: String) = with(user) { OperatorCredentials(uuid, username, display, passwordHash, dateNow()) }
    }
}