package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncErrorsRequest(val syncErrors: List<SyncErrorDto>)

@JsonClass(generateAdapter = true)
data class MarkSyncErrorsResolvedRequest(val syncErrorKeys: List<String>)