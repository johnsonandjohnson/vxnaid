package com.jnj.vaccinetracker.common.data.encryption

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureKeySpecMemoryDataSource @Inject constructor() {
    constructor(initial: SecretKeySpec?) : this() {
        this.secretKeySpec = initial
    }

    private var secretKeySpec: SecretKeySpec? = null
    private val mutex = Mutex()
    suspend fun getValue(): SecretKeySpec? = mutex.withLock {
        secretKeySpec
    }

    suspend fun getOrSetValue(secretKeySpecFactory: suspend () -> SecretKeySpec): SecretKeySpec = mutex.withLock {
        secretKeySpec ?: secretKeySpecFactory().also { this.secretKeySpec = it }
    }
}