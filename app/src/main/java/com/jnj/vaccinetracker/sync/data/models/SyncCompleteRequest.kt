package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncCompleteRequest(val siteUuid: String, val dateSyncCompleted: SyncDate)