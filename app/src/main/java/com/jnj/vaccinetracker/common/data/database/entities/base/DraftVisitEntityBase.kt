package com.jnj.vaccinetracker.common.data.database.entities.base

interface DraftVisitEntityBase : VisitEntityBase, UploadableDraft {
    val locationUuid: String
}