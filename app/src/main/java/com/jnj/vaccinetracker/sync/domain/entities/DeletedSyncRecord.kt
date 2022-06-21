package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.data.models.SyncDate

sealed class DeletedSyncRecord {
    abstract val participantUuid: String
    abstract val dateModified: SyncDate
    val uuid: String get() = visitUuid ?: participantUuid

    open val visitUuid: String? = null

    data class Image(override val participantUuid: String, override val dateModified: SyncDate) : DeletedSyncRecord()

    data class BiometricsTemplate(override val participantUuid: String, override val dateModified: SyncDate) :
        DeletedSyncRecord()

    data class Participant(override val participantUuid: String, override val dateModified: SyncDate, val participantId : String) :
        DeletedSyncRecord()

    data class Visit(
        override val visitUuid: String,
        override val participantUuid: String,
        override val dateModified: SyncDate,
    ) : DeletedSyncRecord()

    val syncEntityType
        get() = when (this) {
            is BiometricsTemplate -> SyncEntityType.BIOMETRICS_TEMPLATE
            is Image -> SyncEntityType.IMAGE
            is Participant -> SyncEntityType.PARTICIPANT
            is Visit -> SyncEntityType.VISIT
        }
}