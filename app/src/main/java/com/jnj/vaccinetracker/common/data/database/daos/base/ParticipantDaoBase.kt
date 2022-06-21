package com.jnj.vaccinetracker.common.data.database.daos.base

import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantDataToUploadModel
import com.jnj.vaccinetracker.common.domain.entities.DraftState

interface ParticipantDaoCommon<E, M> : DaoBase<E> {
    suspend fun findAllByPhone(phone: String): List<@JvmSuppressWildcards M>
    suspend fun findAllByPhoneIsNull(): List<@JvmSuppressWildcards M>
    suspend fun findByParticipantId(participantId: String): M?
    suspend fun findByParticipantUuid(participantUuid: String): M?
    suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int
}

suspend fun ParticipantDaoCommon<*, *>.deleteByParticipantUuid(participantUuid: String): Int = delete(RoomDeleteParticipantModel(participantUuid))

suspend fun <E, M> ParticipantDaoCommon<E, M>.findAllByPhoneNullable(phone: String?): List<@JvmSuppressWildcards M> =
    if (phone == null) findAllByPhoneIsNull() else findAllByPhone(phone)

interface ParticipantDaoBase<E, M> : ParticipantDaoCommon<E, M> {

}

interface DraftParticipantDaoBase<E, M> : ParticipantDaoCommon<E, M> {
    suspend fun findByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): M?
    suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<RoomDraftParticipantDataToUploadModel>
}