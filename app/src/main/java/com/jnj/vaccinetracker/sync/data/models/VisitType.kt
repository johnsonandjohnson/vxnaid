package com.jnj.vaccinetracker.sync.data.models

enum class VisitType(val key: String) {
    IN_PERSON_FOLLOW_UP("In person follow-up"),
    DOSING("Dosing"),
    VIRTUAL_FOLLOW_UP("Virtual follow-up"),
    ENGAGEMENT_1("Engagement 1"),
    ENGAGEMENT_2("Engagement 2"),
    GOODBYE("Goodbye"),
    FACILITY_VISIT("Facility Visit"),
    MEDICINE_REFILL("Medicine Refill"),
    SPUTUM_COLLECTION("Sputum collection"),
    OTHER("Other");

    companion object {
        fun fromKey(key: String) = values().find { it.key == key }
    }
}