package com.jnj.vaccinetracker.common.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FlowExtKtTest : FunSpec({

    val dispatchers = AppCoroutineDispatchers.DEFAULT
    test("awaitAny") {
        val channel = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        val channel2 = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        val doneChannel = MutableStateFlow(false)
        launch(dispatchers.io) {
            listOf(channel, channel2).awaitAny()
            println("done")
            doneChannel.emit(true)
        }
        doneChannel.value shouldBe false
        delay(1000)
        doneChannel.value shouldBe false
        channel2.tryEmit(1)
        println("channel2.tryEmit(1)")
        delay(1000)
        doneChannel.value shouldBe true
        channel.tryEmit(2)
        println("channel1.tryEmit(2)")
        delay(1000)

    }

    context("timeoutAfter") {
        test("when timeout elapses before block then return null") {
            val result = timeoutAfter(100, isCancelTimeout = {
                false
            }) {
                delay(200)
                "test"
            }
            result shouldBe null
        }

        test("when timeout elapses before block and isCancelTimeout returns true then return block result") {
            val result = timeoutAfter(100, isCancelTimeout = {
                true
            }) {
                delay(200)
                "test"
            }
            result shouldBe "test"
        }

        test("when timeout elapses before block and isCancelTimeout returns true 100ms then return block result") {
            val isCancelled = MutableStateFlow(false)
            val jobA = launch {
                val result = timeoutAfter(500, isCancelTimeout = {
                    isCancelled.value
                }) {
                    delay(600)
                    "test"
                }
                result shouldBe "test"
            }
            delay(400)
            isCancelled.value = true
            jobA.join()
        }

        test("when timeout elapses after block then don't wait until timeout finished") {
            val t1 = System.currentTimeMillis()
            val result = timeoutAfter(1000000, isCancelTimeout = {
                false
            }) {
                delay(200)
                "test"
            }
            val t2 = System.currentTimeMillis()
            val t3 = t2 - t1
            t3 shouldBeLessThan 300
            result shouldBe "test"
        }
    }
})
