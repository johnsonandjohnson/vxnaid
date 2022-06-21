package com.jnj.vaccinetracker.common.data.database.entities.base

interface DraftVisitEncounterEntityBase : VisitEntityCommon, UploadableDraft, VisitEncounterBase, ParticipantUuidContainer {
    val locationUuid: String
}