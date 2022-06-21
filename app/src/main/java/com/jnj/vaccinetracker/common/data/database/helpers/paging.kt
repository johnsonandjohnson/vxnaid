package com.jnj.vaccinetracker.common.data.database.helpers

import com.jnj.vaccinetracker.common.helpers.logInfo


suspend fun <T> pagingQueryList(offset: Int = 0, pageSize: Int, queryFunction: suspend (offset: Int, limit: Int) -> List<T>): List<T> {
    val results = queryFunction(offset, pageSize)
    return if (results.isEmpty())
        return results
    else
        results + pagingQueryList(offset = offset + pageSize, pageSize = pageSize, queryFunction)
}


/**
 * load [queryFunction] in chunks to save memory
 */
suspend fun <T> pagingQuery(
    offset: Int = 0, pageSize: Int,
    queryFunction: suspend (offset: Int, limit: Int) -> List<T>,
    onPageResult: suspend (pageResults: List<T>) -> Unit,
) {
    val page = if (offset > 0) offset / pageSize + 1 else 1
    Unit.logInfo("pagingQuery $page")
    val results = queryFunction(offset, pageSize)
    if (results.isEmpty())
        return
    onPageResult(results)
    pagingQuery(offset + pageSize, pageSize, queryFunction, onPageResult)
}