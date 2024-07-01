package com.jnj.vaccinetracker.common.domain.entities

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

typealias SubstancesConfig = List<Substance>

fun Moshi.substancesConfigAdapter(): JsonAdapter<SubstancesConfig> = adapter(Types.newParameterizedType(List::class.java, Substance::class.java))

@JsonClass(generateAdapter = true)
data class Substance(
    val conceptUuid: String,
    val weeksAfterBirth: Int,
    val weeksAfterBirthLowWindow: Int,
    val weeksAfterBirthUpWindow: Int,
    val name: String,
    val group: String,
    val routeOfAdministration: String,
    val category: String
)