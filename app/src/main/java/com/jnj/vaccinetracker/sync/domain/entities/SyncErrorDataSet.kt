package com.jnj.vaccinetracker.sync.domain.entities

data class SyncErrorDataSet(val offset: Int, val limit: Int, val results: List<SyncErrorMetadata>) {
    val page get() = offset / limit + 1

    fun hasNext() = results.isNotEmpty()

    fun nextPageRequest(): FindAllSyncErrorMetadata? {
        return if (hasNext()) FindAllSyncErrorMetadata(offset + limit, limit) else null
    }
}