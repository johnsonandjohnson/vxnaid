package com.jnj.vaccinetracker.common.data.helpers

import com.jnj.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

const val CONCURRENCY_DEFAULT = 2

suspend fun <T, R> runTasks(
    totalDataSet: List<T>,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    concurrency: Int = CONCURRENCY_DEFAULT,
    debugLabel: String,
    progressUpdate: suspend (taskParams: T, results: R, index: Int, progress: Int, max: Int) -> Unit = { _, _, _, _, _ -> },
    runTask: suspend (T) -> R,
): List<R> = withContext(coroutineContext) {
    val mutex = Mutex()
    var progress = 0
    val max = totalDataSet.size
    logInfo("$debugLabel -- runTasks: $max")
    val resultList = mutableListOf<Pair<Int, R>>()

    /**
     * create [concurrency] amount of deferred coroutines in parallel and loop through data
     */
    val dataSetSplitByConcurrency = totalDataSet.withIndex().chunked((max / concurrency).coerceAtLeast(1))
    dataSetSplitByConcurrency.map { dataSetChunks ->
        async(coroutineContext) {
            dataSetChunks.forEach { (i, dataSet) ->
                val result = runTask(dataSet)
                mutex.withLock {
                    progressUpdate(dataSet, result, i, ++progress, max)
                    logInfo("$debugLabel -- runTask progressUpdate: $i $progress/$max")
                    resultList += (i to result)
                }
            }
        }
    }.forEach { it.await() }
    resultList.sortedBy { it.first }.map { it.second }
}


/**
 * standard [delay] function doesn't work very well and can't be trusted.
 * This one has a better guarantee to return right after [millis]
 * after each [intervalMillis], it will check if [millis] has expired based on current time
 */
suspend fun delaySafe(millis: Long, intervalMillis: Long = 333) {
    val t1 = System.currentTimeMillis()
    while (true) {
        // 3 arithmetic operations per minute
        delay(intervalMillis)
        val t2 = System.currentTimeMillis()
        if (t1 + millis < t2)
            break
    }
}