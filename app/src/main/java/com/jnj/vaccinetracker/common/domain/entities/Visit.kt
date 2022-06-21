package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.models.Constants


sealed class VisitBase {
    abstract val startDatetime: DateEntity
    abstract val participantUuid: String
    abstract val visitUuid: String
    abstract val attributes: Map<String, String>
    abstract val visitType: String
}

data class Visit(
    override val startDatetime: DateEntity,
    override val participantUuid: String,
    override val visitUuid: String,
    override val attributes: Map<String, String>,
    val observations: Map<String, ObservationValue>,
    val dateModified: DateEntity,
    override val visitType: String,
) : VisitBase()

data class DraftVisit(
    override val startDatetime: DateEntity,
    override val participantUuid: String,
    val locationUuid: String,
    override val visitUuid: String,
    override val attributes: Map<String, String>,
    override val visitType: String,
    val draftState: DraftState,
) : VisitBase() {
    val isOtherVisit: Boolean = visitType == Constants.VISIT_TYPE_OTHER
}

data class DraftVisitEncounter(
    override val startDatetime: DateEntity,
    override val participantUuid: String,
    val locationUuid: String,
    override val visitUuid: String,
    override val attributes: Map<String, String>,
    val observations: Map<String, String>,
    val draftState: DraftState,
) : VisitBase() {
    val observationsWithDate get() = observations.map { it.key to ObservationValue(it.value, startDatetime) }.toMap()

    override val visitType: String get() = Constants.VISIT_TYPE_DOSING
}

