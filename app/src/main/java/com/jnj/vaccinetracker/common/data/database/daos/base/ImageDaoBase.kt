package com.jnj.vaccinetracker.common.data.database.daos.base

import com.jnj.vaccinetracker.common.data.database.helpers.pagingQuery

interface ImageDaoBase<T> : DaoBase<T> {

    suspend fun findAll(offset: Int, limit: Int): List<@JvmSuppressWildcards T>
}

private const val SELECT_ALL_LIMIT = 5000

suspend fun <E> ImageDaoBase<E>.forEachAll(pageSize: Int = SELECT_ALL_LIMIT, onPageResult: suspend (items: List<E>) -> Unit) {
    pagingQuery(pageSize = pageSize, queryFunction = { offset, limit -> findAll(offset, limit) }, onPageResult = onPageResult)
}