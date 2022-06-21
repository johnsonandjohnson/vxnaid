package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.JsonClass

typealias ActiveUsersResponse = List<ActiveUser>

@JsonClass(generateAdapter = true)
data class ActiveUser(
    val uuid: String,
    val display: String,
    val username: String,
)