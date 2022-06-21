package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.domain.entities.Site
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class SyncRequest(
    /**
     * UTC epoch millis
     * device date is not permitted, only last date from the response should be specified
     * Action: return records with a date modified greater or equal than [dateModifiedOffset]
     */
    val dateModifiedOffset: SyncDate?,
    /**
     * Action: filter records to site or country level
     */
    val syncScope: SyncScopeDto,
    /**
     * Action: exclude from response if record.dateModified == [dateModifiedOffset] and record.uuid in [uuidsWithDateModifiedOffset]
     */
    val uuidsWithDateModifiedOffset: List<String>,
    /**
     * Action: maximally return x amount of records
     */
    val limit: Int,
    /**
     * Action: if **true** save bandwidth by not returning records created by specified deviceId
     */
    val optimize: Boolean,
) {
    init {
        require(limit > 0)
    }

    val offset: Int get() = uuidsWithDateModifiedOffset.size
}

@JsonClass(generateAdapter = true)
data class SyncScopeDto(val country: String, val cluster: String?, val siteUuid: String?)

fun Site.toSyncScopeDto(syncScopeLevel: SyncScopeLevel): SyncScopeDto {
    return when (syncScopeLevel) {
        SyncScopeLevel.COUNTRY -> SyncScopeDto(country = country, siteUuid = null, cluster = null)
        SyncScopeLevel.SITE -> SyncScopeDto(country = country, siteUuid = uuid, cluster = cluster)
        SyncScopeLevel.CLUSTER -> SyncScopeDto(country = country, cluster = cluster, siteUuid = null)
    }
}