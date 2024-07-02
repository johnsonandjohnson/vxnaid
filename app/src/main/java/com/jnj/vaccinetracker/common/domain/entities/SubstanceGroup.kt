package com.jnj.vaccinetracker.common.domain.entities

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

typealias SubstancesGroupConfig = List<SubstanceGroup>

fun Moshi.substancesGroupConfigAdapter(): JsonAdapter<SubstancesGroupConfig> = adapter(Types.newParameterizedType(List::class.java, SubstanceGroup::class.java))

@JsonClass(generateAdapter = true)
data class SubstanceGroup(
    val substanceName: String,
    val options: List<String>
)