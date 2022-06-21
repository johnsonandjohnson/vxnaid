package com.jnj.vaccinetracker.sync.domain.usecases.store.base

import com.jnj.vaccinetracker.sync.data.models.SyncRecordBase

interface StoreSyncRecordUseCaseBase<T : SyncRecordBase, R> {

    /**
     * if a result is returned then the record is saved in the database.
     * @throws [Exception] indicates record is not saved and stored assets files are removed
     */
    suspend fun store(syncRecord: T)
}