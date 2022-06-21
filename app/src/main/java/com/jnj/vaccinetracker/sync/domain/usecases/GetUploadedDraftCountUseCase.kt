package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.base.UpdatableDraftRepository
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import javax.inject.Inject


class GetUploadedDraftCountUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
) {

    private fun SyncEntityType.repo(): UpdatableDraftRepository<*> = when (this) {
        SyncEntityType.PARTICIPANT -> draftParticipantRepository
        SyncEntityType.IMAGE -> draftParticipantImageRepository
        SyncEntityType.BIOMETRICS_TEMPLATE -> draftParticipantBiometricsTemplateRepository
        SyncEntityType.VISIT -> draftVisitRepository
    }

    suspend fun getCount(syncEntityType: SyncEntityType): Long {
        return syncEntityType.repo().countByDraftState(DraftState.UPLOADED)
    }
}