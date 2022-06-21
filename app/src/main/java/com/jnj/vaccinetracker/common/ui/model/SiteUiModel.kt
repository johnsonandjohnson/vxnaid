package com.jnj.vaccinetracker.common.ui.model

import com.jnj.vaccinetracker.common.domain.entities.Site
import com.jnj.vaccinetracker.common.domain.entities.TranslationMap

data class SiteUiModel(val site: Site, val displayName: String, val displayCountry: String) {
    val name get() = site.name
    val country get() = site.country
    val uuid get() = site.uuid
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SiteUiModel

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return displayName
    }

    companion object {
        fun create(site: Site, loc: TranslationMap) = SiteUiModel(site, displayName = loc[site.name], displayCountry = loc[site.country])
    }
}