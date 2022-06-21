package com.jnj.vaccinetracker.sync.domain.factories

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.domain.usecases.download.DownloadParticipantBiometricsTemplateSyncRecordsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.DownloadParticipantImageSyncRecordsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.DownloadParticipantSyncRecordsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.DownloadVisitSyncRecordsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCase
import javax.inject.Inject

class DownloadSyncRecordsUseCaseFactory @Inject constructor(
    private val downloadParticipantSyncRecordsUseCase: DownloadParticipantSyncRecordsUseCase,
    private val downloadParticipantImageSyncRecordsUseCase: DownloadParticipantImageSyncRecordsUseCase,
    private val downloadVisitSyncRecordsUseCase: DownloadVisitSyncRecordsUseCase,
    private val downloadParticipantBiometricsTemplateSyncRecordsUseCase: DownloadParticipantBiometricsTemplateSyncRecordsUseCase,
) {

    fun create(syncEntityType: SyncEntityType): DownloadSyncRecordsUseCase {
        return when (syncEntityType) {
            SyncEntityType.PARTICIPANT -> downloadParticipantSyncRecordsUseCase
            SyncEntityType.IMAGE -> downloadParticipantImageSyncRecordsUseCase
            SyncEntityType.BIOMETRICS_TEMPLATE -> downloadParticipantBiometricsTemplateSyncRecordsUseCase
            SyncEntityType.VISIT -> downloadVisitSyncRecordsUseCase
        }
    }
}