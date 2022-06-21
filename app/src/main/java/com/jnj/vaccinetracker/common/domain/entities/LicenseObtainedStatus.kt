package com.jnj.vaccinetracker.common.domain.entities

enum class LicenseObtainedStatus {
    /**
     * when NLicense.obtain function returns **true**
     */
    OBTAINED,

    /**
     * when NLicense.obtain function returns **false** because another license is still active
     */
    OBTAINABLE_AFTER_FORCE_CLOSE,

    /**
     * when NLicense.obtain function returns **false**
     */
    NOT_OBTAINED;

    val isObtained: Boolean get() = this == OBTAINED

    infix fun and(other: LicenseObtainedStatus): LicenseObtainedStatus {
        return when (other) {
            OBTAINED -> this
            OBTAINABLE_AFTER_FORCE_CLOSE -> other
            NOT_OBTAINED -> other
        }
    }
}