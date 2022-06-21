package com.jnj.vaccinetracker.common.helpers

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class SessionExpiryObserver @Inject constructor() {

    private val sessionExpiredMutable = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sessionExpiredEvents get() = sessionExpiredMutable.asSharedFlow()

    fun notifySessionExpired() {
        sessionExpiredMutable.tryEmit(Unit)
    }
}