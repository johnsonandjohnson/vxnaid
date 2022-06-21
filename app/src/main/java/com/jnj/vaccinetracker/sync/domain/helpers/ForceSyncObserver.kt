package com.jnj.vaccinetracker.sync.domain.helpers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForceSyncObserver @Inject constructor() {
    private val syncNowFlow = MutableSharedFlow<Unit>(0, 10)

    fun forceSync() {
        syncNowFlow.tryEmit(Unit)
    }

    fun observeForceSync(): Flow<Unit> = syncNowFlow
}