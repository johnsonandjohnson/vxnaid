package com.jnj.vaccinetracker.common.domain.entities


data class ParticipantIdentificationCriteria(
    val participantId: String?,
    val phone: String?,
    val biometricsTemplate: BiometricsTemplateBytes?,
) {
    init {
        require(!participantId.isNullOrEmpty() || !phone.isNullOrEmpty() || biometricsTemplate != null)
    }

    val isTemplateOnly = phone.isNullOrEmpty() && participantId.isNullOrEmpty() && biometricsTemplate != null
}