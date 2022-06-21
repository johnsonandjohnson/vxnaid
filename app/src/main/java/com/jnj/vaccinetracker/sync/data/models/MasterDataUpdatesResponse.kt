package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.JsonClass

typealias MasterDataUpdatesResponse = List<MasterDataUpdateEntryDto>

@JsonClass(generateAdapter = true)
data class MasterDataUpdateEntryDto(
    val name: String,
    /**
     * md5 hash
     */
    val hash: String?,
    /**
     * epoch
     */
    val dateModified: SyncDate?,
)