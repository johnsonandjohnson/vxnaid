package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.data.models.SyncDate

sealed class FailedSyncRecordDownload {
    abstract val participantUuid: String
    abstract val dateModified: SyncDate
    abstract val dateLastDownloadAttempt: DateEntity
    val uuid: String get() = visitUuid ?: participantUuid

    open val visitUuid: String? = null

    abstract fun refreshLastDownloadAttemptDate(dateLastDownloadAttempt: DateEntity = dateNow()): FailedSyncRecordDownload

    data class Image(override val participantUuid: String, override val dateModified: SyncDate, override val dateLastDownloadAttempt: DateEntity) : FailedSyncRecordDownload() {
        override fun refreshLastDownloadAttemptDate(dateLastDownloadAttempt: DateEntity) = copy(dateLastDownloadAttempt = dateLastDownloadAttempt)
    }

    data class BiometricsTemplate(override val participantUuid: String, override val dateModified: SyncDate, override val dateLastDownloadAttempt: DateEntity) :
        FailedSyncRecordDownload() {
        override fun refreshLastDownloadAttemptDate(dateLastDownloadAttempt: DateEntity) = copy(dateLastDownloadAttempt = dateLastDownloadAttempt)
    }

    data class Participant(override val participantUuid: String, override val dateModified: SyncDate, override val dateLastDownloadAttempt: DateEntity) :
        FailedSyncRecordDownload() {
        override fun refreshLastDownloadAttemptDate(dateLastDownloadAttempt: DateEntity) = copy(dateLastDownloadAttempt = dateLastDownloadAttempt)
    }

    data class Visit(
        override val visitUuid: String,
        override val participantUuid: String,
        override val dateModified: SyncDate,
        override val dateLastDownloadAttempt: DateEntity,
    ) : FailedSyncRecordDownload() {
        override fun refreshLastDownloadAttemptDate(dateLastDownloadAttempt: DateEntity) = copy(dateLastDownloadAttempt = dateLastDownloadAttempt)
    }

    val syncEntityType
        get() = when (this) {
            is BiometricsTemplate -> SyncEntityType.BIOMETRICS_TEMPLATE
            is Image -> SyncEntityType.IMAGE
            is Participant -> SyncEntityType.PARTICIPANT
            is Visit -> SyncEntityType.VISIT
        }
}