package com.jnj.vaccinetracker.participantflow.model

import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.ui.model.DisplayValue

data class ParticipantUiModel(
    val participantUUID: String?,
    val participantId: String?,
    val irisMatchingScore: Int?,
    val birthDateText: String?,
    val isBirthDateEstimated: Boolean?,
    val gender: Gender?,
    val telephone: String?,
    val homeLocation: String?,
    val vaccine: DisplayValue?,
    val siteUUID: String?,
) {

    constructor(
        participantId: String?,
        matchingScore: Int?,
        siteUUID: String?,
    ) : this(null, participantId, matchingScore, null, null, null, null, null, null, siteUUID)

}