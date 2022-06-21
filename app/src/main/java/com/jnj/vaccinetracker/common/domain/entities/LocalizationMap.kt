package com.jnj.vaccinetracker.common.domain.entities

data class LocalizationMap(val languages: LanguageMap)

data class LanguageMap(private val map: Map<String, TranslationMap>) {
    fun getTranslationsByLanguage(lang: String, fallbackLang: String): TranslationMap? = map[lang] ?: map[fallbackLang]
}

data class TranslationMap(private val map: Map<String, String>) {
    operator fun get(value: String): String = map[value] ?: value
}