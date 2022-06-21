package com.jnj.vaccinetracker.common.domain.entities

enum class SyncEntityType {
    PARTICIPANT, IMAGE, BIOMETRICS_TEMPLATE, VISIT;

    val code get() = name

    companion object {
        fun fromCode(code: String) = values().find { it.code == code }
    }
}