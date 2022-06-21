package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.data.database.repositories.SyncScopeRepository
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSyncScopeUseCase @Inject constructor(
    private val buildSyncScopeUseCase: BuildSyncScopeUseCase,
    private val syncScopeRepository: SyncScopeRepository,
    private val replaceSyncScopeUseCase: ReplaceSyncScopeUseCase,
    private val readyForMigrationSignaler: ReadyForMigrationSignaler,
) {
    //we don't want concurrent migrations so use a lock
    private val mutex = Mutex()

    suspend fun getSyncScope(): SyncScope = mutex.withLock {
        val existingSyncScope = syncScopeRepository.findOne()
        val newSyncScope = buildSyncScopeUseCase.buildSyncScope()
        if (existingSyncScope == null || !existingSyncScope.isIdenticalTo(newSyncScope)) {
            readyForMigrationSignaler.awaitReady()
            replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
            val insertedSyncScope = syncScopeRepository.findOne()
            check(insertedSyncScope == newSyncScope) { "newSyncScope [$newSyncScope] is not saved, instead we got [$insertedSyncScope]" }
        }
        newSyncScope
    }
}