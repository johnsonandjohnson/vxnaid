package com.jnj.vaccinetracker.common.data.database.mappers

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.models.SyncErrorDto
import com.jnj.vaccinetracker.sync.data.models.SyncErrorsDto
import com.jnj.vaccinetracker.sync.data.models.toDto
import com.jnj.vaccinetracker.sync.domain.entities.SyncError
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import okio.BufferedSink
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class SyncErrorJsonMapper @Inject constructor(private val moshi: Moshi, private val dispatchers: AppCoroutineDispatchers) {
    private val syncMetadataJsonAdapter by lazy { moshi.adapter(SyncErrorMetadata::class.java) }
    private val syncErrorJsonAdapter by lazy { moshi.adapter(SyncErrorDto::class.java) }
    private val syncErrorsJsonAdapter by lazy { moshi.adapter(SyncErrorsDto::class.java) }
    suspend fun toJson(metadata: SyncErrorMetadata): String = withContext(dispatchers.io) {
        syncMetadataJsonAdapter.toJson(metadata)
    }

    suspend fun fromJson(json: String): SyncErrorMetadata = withContext(dispatchers.io) {
        syncMetadataJsonAdapter.fromJson(json) ?: error("$json is null")
    }

    fun writeToSink(syncErrorsDto: SyncErrorsDto, sink: BufferedSink) {
        syncErrorsJsonAdapter.toJson(sink, syncErrorsDto)
    }

    suspend fun appendToWriter(jsonWriter: JsonWriter, syncError: SyncError) = withContext(dispatchers.io) {
        syncErrorJsonAdapter.toJson(jsonWriter, syncError.toDto())
    }
}