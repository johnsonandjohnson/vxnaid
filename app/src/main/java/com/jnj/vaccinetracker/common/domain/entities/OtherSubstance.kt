package com.jnj.vaccinetracker.common.domain.entities

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

typealias OtherSubstancesConfig = List<OtherSubstance>

fun Moshi.otherSubstancesConfigAdapter(): JsonAdapter<OtherSubstancesConfig> = adapter(Types.newParameterizedType(List::class.java, OtherSubstance::class.java))

@JsonClass(generateAdapter = true)
data class OtherSubstance(
    val conceptName: String,
    val label: String,
    val weeksAfterBirth: Int,
    val weeksAfterBirthLowWindow: Int,
    val weeksAfterBirthUpWindow: Int,
    val category: String,
    val options: List<String>
)