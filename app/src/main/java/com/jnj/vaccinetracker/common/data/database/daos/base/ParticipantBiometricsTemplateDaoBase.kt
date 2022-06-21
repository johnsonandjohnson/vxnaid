package com.jnj.vaccinetracker.common.data.database.daos.base

import com.jnj.vaccinetracker.common.data.database.helpers.pagingQuery
import com.jnj.vaccinetracker.common.data.database.helpers.pagingQueryList
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel

interface ParticipantBiometricsTemplateDaoBase<E> : DaoBase<E> {
    suspend fun findAllByPhone(phone: String): List<@JvmSuppressWildcards E>
    suspend fun findAllByPhoneIsNull(offset: Int, limit: Int): List<@JvmSuppressWildcards E>
    suspend fun findByParticipantUuid(participantUuid: String): E?
    suspend fun findByParticipantId(participantId: String): E?
    suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int
    suspend fun findAll(offset: Int, limit: Int): List<@JvmSuppressWildcards E>
}

suspend fun ParticipantBiometricsTemplateDaoBase<*>.deleteByParticipantUuid(participantUuid: String): Int = delete(RoomDeleteParticipantModel(participantUuid))

private const val SELECT_ALL_LIMIT = 5000
suspend fun <E> ParticipantBiometricsTemplateDaoBase<E>.findAllByPhoneIsNull(): List<E> {
    return pagingQueryList(pageSize = SELECT_ALL_LIMIT) { offset, limit -> findAllByPhoneIsNull(offset, limit) }
}

suspend fun <E> ParticipantBiometricsTemplateDaoBase<E>.findAllByPhoneNullable(phone: String?): List<@JvmSuppressWildcards E> =
    if (phone == null) findAllByPhoneIsNull() else findAllByPhone(phone)


suspend fun <E> ParticipantBiometricsTemplateDaoBase<E>.findAll(): List<E> {
    return pagingQueryList(pageSize = SELECT_ALL_LIMIT) { offset, limit -> findAll(offset, limit) }
}

suspend fun <E> ParticipantBiometricsTemplateDaoBase<E>.forEachAll(pageSize: Int = SELECT_ALL_LIMIT, onPageResult: suspend (items: List<E>) -> Unit) {
    pagingQuery(pageSize = pageSize, queryFunction = { offset, limit -> findAll(offset, limit) }, onPageResult = onPageResult)
}