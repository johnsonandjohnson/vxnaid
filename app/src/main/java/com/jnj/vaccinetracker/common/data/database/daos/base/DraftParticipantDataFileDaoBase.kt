package com.jnj.vaccinetracker.common.data.database.daos.base

import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.domain.entities.DraftState

interface DraftParticipantDataFileDaoBase<E> {
    suspend fun findAllByDraftState(draftState: DraftState, offset: Int, limit: Int): List<@JvmSuppressWildcards E>
    suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int
}