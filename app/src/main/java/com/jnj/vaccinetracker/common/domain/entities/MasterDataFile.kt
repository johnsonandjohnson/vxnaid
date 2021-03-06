package com.jnj.vaccinetracker.common.domain.entities

enum class MasterDataFile {
    CONFIGURATION, SITES, LOCALIZATION, ADDRESS_HIERARCHY, VACCINE_SCHEDULE;

    val fileName
        get() = when (this) {
            CONFIGURATION -> "configuration.json"
            SITES -> "sites.json"
            LOCALIZATION -> "localization.json"
            ADDRESS_HIERARCHY -> "address_hierarchy.json"
            VACCINE_SCHEDULE -> "vaccine_schedule.json"
        }

    val isEncrypted: Boolean
        get() = when (this) {
            CONFIGURATION -> true
            SITES -> true
            LOCALIZATION -> true
            ADDRESS_HIERARCHY -> true
            VACCINE_SCHEDULE -> true
        }

    val syncName
        get() = when (this) {
            CONFIGURATION -> "config"
            SITES -> "locations"
            LOCALIZATION -> "localization"
            ADDRESS_HIERARCHY -> "addressHierarchy"
            VACCINE_SCHEDULE -> "vaccineSchedule"
        }
}