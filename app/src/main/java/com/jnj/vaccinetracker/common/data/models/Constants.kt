package com.jnj.vaccinetracker.common.data.models

import com.jnj.vaccinetracker.sync.data.models.VisitType

/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
object Constants {

    const val IRIS_TEMPLATE_NAME = "irisTemplate.dat"

    // Participant
    const val ATTRIBUTE_LOCATION = "LocationAttribute"
    const val ATTRIBUTE_LANGUAGE = "personLanguage"
    const val ATTRIBUTE_TELEPHONE = "Telephone Number"
    const val ATTRIBUTE_VACCINE = "Vaccination program"
    const val ATTRIBUTE_ORIGINAL_PARTICIPANT_ID = "originalParticipantId"
    const val ATTRIBUTE_IS_BIRTH_DATE_ESTIMATED = "Is Birth Date Estimated"
    const val ATTRIBUTE_BIRTH_WEIGHT= "Birth Weight"

    // Visit
    const val ATTRIBUTE_VISIT_STATUS = "Visit Status"
    const val ATTRIBUTE_VISIT_DAYS_AFTER = "Up Window"
    const val ATTRIBUTE_VISIT_DAYS_BEFORE = "Low Window"
    const val ATTRIBUTE_VISIT_VACCINE_MANUFACTURER = "Vaccine Manufacturer"
    const val ATTRIBUTE_VISIT_DOSE_NUMBER = "Dose number"
    const val ATTRIBUTE_VISIT_WEIGHT = "Weight"
    const val ATTRIBUTE_VISIT_HEIGHT = "Height"
    const val ATTRIBUTE_VISIT_MUAC = "Muac"
    const val ATTRIBUTE_VISIT_OEDEMA = "Oedema"
    const val VISIT_TYPE_DOSING = "Dosing"
    const val VISIT_TYPE_OTHER = "Other"
    const val VISIT_STATUS_OCCURRED = "OCCURRED"
    const val VISIT_STATUS_MISSED = "MISSED"
    const val VISIT_STATUS_SCHEDULED = "SCHEDULED"
    const val OBSERVATION_TYPE_BARCODE = "Barcode"
    const val OBSERVATION_TYPE_MANUFACTURER = "Vaccine Manufacturer"

    // common attributes
    const val ATTRIBUTE_OPERATOR = "operatorUuid"

    /**
     * upcoming in person visit types
     */
    val SUPPORTED_UPCOMING_VISIT_TYPES = listOf(VisitType.DOSING, VisitType.IN_PERSON_FOLLOW_UP, VisitType.OTHER)

    //  ROLES
    const val ROLE_SYNC_ADMIN = "Sync Admin"
    const val ROLE_OPERATOR = "Operator"

    // PROGRESS
    const val MAX_PERCENT = 100

    // MOVE TO THE CONFIGURATION
    val VISIT_TYPES_DROPDOWN = listOf(
       "At birth",
        "6 weeks",
        "10 weeks",
        "14 weeks",
        "6 months",
        "9 months",
        "12 months",
        "18 months"
    )
}