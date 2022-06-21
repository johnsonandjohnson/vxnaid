package com.jnj.vaccinetracker.sync.data.models

import okhttp3.Credentials

data class SyncUserCredentials(val username: String, val password: String) {
    fun basicAuth() = Credentials.basic(username = username, password = password)
    override fun toString(): String {
        return "$username:$password"
    }
}