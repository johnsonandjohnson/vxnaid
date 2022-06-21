package com.jnj.vaccinetracker.common.domain.entities

data class BiometricsFileMatch(
    val template: ParticipantBiometricsTemplateFileBase,
    val matchingScore: Int,
) {
    val uuid get() = template.participantUuid
}