package com.jnj.vaccinetracker.common.data.database.helpers

private const val MAX_BIND_PARAMETER_CNT = 999


suspend fun <T, ID> chunkedQueryByIds(allIds: List<ID>, queryFunction: suspend (ids: List<ID>) -> T): List<T> {
    return allIds.chunked(MAX_BIND_PARAMETER_CNT).map { ids -> queryFunction(ids) }
}

suspend fun <ID, T> chunkedQueryListByIds(allIds: List<ID>, queryFunction: suspend (ids: List<ID>) -> List<T>): List<T> {
    return allIds.chunked(MAX_BIND_PARAMETER_CNT).map { queryFunction(it) }.flatten()
}