package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.exceptions.SiteNotFoundException
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Sites(val results: List<Site>) {
    fun findByUuid(uuid: String) = results.find { it.uuid == uuid }
    fun findByUuidOrThrow(uuid: String) = findByUuid(uuid) ?: throw SiteNotFoundException(uuid)
}

@JsonClass(generateAdapter = true)
data class Site(
    val uuid: String,
    val name: String,
    val country: String,
    val cluster: String,
    val countryCode: String,
    val siteCode: String,
) {
    override fun toString() = name
}