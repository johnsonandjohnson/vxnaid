package com.jnj.vaccinetracker.common.helpers

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionUpdateObserver @Inject constructor() {

    private val versionUpdateAvailableMutable = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val versionUpdateEvents get() = versionUpdateAvailableMutable.asSharedFlow()

    fun notifyUpdateAvailable() {
        versionUpdateAvailableMutable.tryEmit(Unit)
    }
}