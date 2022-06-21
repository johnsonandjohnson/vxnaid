package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.Json

enum class SyncScopeLevel(val code: String) {
    @field:Json(name = "country")
    COUNTRY("country"),

    @field:Json(name = "site")
    SITE("site"),

    @field:Json(name = "cluster")
    CLUSTER("cluster");

    val isSite get() = this == SITE
    val isCountry get() = this == COUNTRY

    companion object {
        fun fromCode(code: String) = values().find { it.code == code }
    }
}



