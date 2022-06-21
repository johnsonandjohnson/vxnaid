package com.jnj.vaccinetracker.common.data.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class CoroutinesHelpersKtTest : FunSpec({

    context("runTasks") {
        listOf(1, 2, 3).forEach { concurrency ->
            test("given concurrency=$concurrency and 10 tasks then run all of them in batches") {
                val dataSet = generateSequence(0) { it + 1 }.take(10).map { it to it + 1000 }.toList()

                val debugLabel = "test"
                val results = runTasks(dataSet, concurrency = concurrency, debugLabel = debugLabel) { (_, result) ->
                    delay(20)
                    result
                }
                results shouldBe dataSet.map { it.second }
            }
        }
    }

    context("delaySafe") {
        listOf<Long>(1000, 1500, 2000).forEach { d ->
            val limit = d * 2L
            test("given param ${d}ms then return before ${limit}ms") {
                val t1 = System.currentTimeMillis()
                delaySafe(d)
                val t2 = System.currentTimeMillis()
                val difference = t2 - t1
                println("$difference for $d")
                difference shouldBeLessThan limit
            }
        }
    }
})
