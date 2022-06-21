package com.jnj.vaccinetracker.common.helpers

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


suspend fun <T> Flow<T>.await(): T = first()


@OptIn(FlowPreview::class)
fun <T> Collection<Flow<T>>.flattenMerge() = flowOf(*toTypedArray()).flattenMerge()

/**
 * suspend until any of the [Flow] in the list completes
 */
suspend fun List<Flow<*>>.awaitAny(coroutineContext: CoroutineContext = EmptyCoroutineContext) {
    flattenMerge()
        .flowOn(coroutineContext)
        .await()
}

/**
 * suspend until any of the [Flow] in the list returns [T]
 */
suspend fun <T> List<Flow<T>>.awaitFirst(): T {
    return flattenMerge().await()
}

/**
 * after [timeout] elapses, if [isCancelTimeout] returns false then null will be returned otherwise we will wait for the result of [block]
 */
suspend fun <T> timeoutAfter(timeout: Long, isCancelTimeout: suspend () -> Boolean, block: suspend () -> T?): T? {
    return listOf(flow {
        emit(block())
    }, flow {
        delay(timeout)
        if (!isCancelTimeout())
            emit(null)
    }).awaitFirst()
}