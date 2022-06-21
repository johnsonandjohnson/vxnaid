package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.api.response.AddressDto
import com.squareup.moshi.Json

data class ParticipantMatch(
    val uuid: String,
    val participantId: String,
    val matchingScore: Int?,
    val gender: Gender,
    val birthDate: BirthDate,
    @Json(name = "addresses") val address: AddressDto?,
    val attributes: Map<String, String>,
) {
    val locationUuid: String?
        get() = attributes[Constants.ATTRIBUTE_LOCATION]

    val vaccine: String?
        get() = attributes[Constants.ATTRIBUTE_VACCINE]

    val personLanguage: String?
        get() = attributes[Constants.ATTRIBUTE_LANGUAGE]

    val telephoneNumber: String?
        get() = attributes[Constants.ATTRIBUTE_TELEPHONE]

    val yearOfBirth: Int = birthDate.year

    fun isBiometricsMatch(biometricsTemplateBytes: BiometricsTemplateBytes?) = biometricsTemplateBytes != null && (matchingScore ?: 0) > 0

    fun isPhoneMatch(phoneCriteria: String?) = !phoneCriteria.isNullOrEmpty() && telephoneNumber == phoneCriteria

    fun isParticipantIdMatch(participantIdCriteria: String?) = !participantIdCriteria.isNullOrEmpty() && participantId.equals(participantIdCriteria, ignoreCase = true)
}

