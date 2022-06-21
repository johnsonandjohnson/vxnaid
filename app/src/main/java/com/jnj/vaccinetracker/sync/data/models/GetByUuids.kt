package com.jnj.vaccinetracker.sync.data.models

import com.squareup.moshi.JsonClass

interface GetByParticipantUuids {
    val participantUuids: List<String>
}

interface GetByVisitUuids {
    val visitUuids: List<String>
}

@JsonClass(generateAdapter = true)
data class GetParticipantsByUuidsRequest(override val participantUuids: List<String>) : GetByParticipantUuids

@JsonClass(generateAdapter = true)
data class GetImagesByUuidsRequest(override val participantUuids: List<String>) : GetByParticipantUuids

@JsonClass(generateAdapter = true)
data class GetBiometricsTemplatesByUuidsRequest(override val participantUuids: List<String>) : GetByParticipantUuids

@JsonClass(generateAdapter = true)
data class GetVisitsByUuidsRequest(override val visitUuids: List<String>) : GetByVisitUuids