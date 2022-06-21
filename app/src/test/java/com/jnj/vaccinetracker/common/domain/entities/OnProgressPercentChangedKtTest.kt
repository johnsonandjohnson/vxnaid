package com.jnj.vaccinetracker.common.domain.entities

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OnProgressPercentChangedKtTest : FunSpec({


    context("percent") {
        mapOf(-1 to 0, 0 to 0, 101 to 100, 1000 to 100).forEach { (param, expected) ->
            test("given $param then return $expected") {
                param.percent shouldBe expected
            }
        }
    }

    context("progress") {
        test("given zero weight then throw exception") {
            runCatching {
                1.progress(0)
            }.isFailure shouldBe true
        }

        test("given negative weight then throw exception") {
            runCatching {
                1.progress(-1)
            }.isFailure shouldBe true
        }

        test("given positive weight then return normally") {
            10.progress(weight = 30) shouldBe Progress(10, 30)
        }
        context("with abnormal percent") {
            listOf(-1, 101).forEach { abnormalPercent ->
                test("given $abnormalPercent then return $abnormalPercent ") {
                    abnormalPercent.progress().progressPercent shouldBe abnormalPercent
                }
            }
        }


    }
    context("combineProgressPercent") {
        data class TestData(val givenParams: List<Progress>, val expectedResult: Int)
        listOf(
            TestData(listOf(0.progress()), 0),
            TestData(listOf(25.progress()), 25),
            TestData(listOf(100.progress()), 100),
            TestData(listOf(1000.progress()), 100),
            TestData(listOf((-1).progress()), 0),
            TestData(listOf(100.progress(), 100.progress()), 100),
            TestData(listOf(100.progress(), 50.progress()), 75),
            TestData(listOf(1.progress(), 100.progress()), 50),
            TestData(listOf(100.progress(weight = 20), 0.progress(weight = 80)), 20),
        ).forEach { (givenParams, expectedResult) ->
            test("given $givenParams then return $expectedResult") {
                combineProgressPercent(*givenParams.toTypedArray()) shouldBe expectedResult
            }
        }

    }
})
