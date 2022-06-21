package com.jnj.vaccinetracker.common.data.models.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocalizationMapDto(val localization: LanguageMap)

private typealias LanguageMap = Map<String, TranslationMap>
private typealias TranslationMap = Map<String, String>