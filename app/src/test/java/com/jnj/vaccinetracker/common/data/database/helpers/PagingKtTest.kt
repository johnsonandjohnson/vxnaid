package com.jnj.vaccinetracker.common.data.database.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class PagingKtTest : FunSpec({


    context("pagingQuery") {
        val dataSet = listOf(4, 3, 1, 7, 9, 8, 5)

        fun List<Int>.queryFunc(offset: Int, limit: Int) = drop(offset).take(limit)

        suspend fun List<Int>.pagQuery(offset: Int, limit: Int): List<List<Int>> {
            val results = mutableListOf<List<Int>>()
            pagingQuery(offset, limit, ::queryFunc) { pageResults ->
                results += pageResults
            }
            return results
        }

        class OffsetTestData(val offset: Int, val firstResult: Int)
        listOf(OffsetTestData(offset = 0, firstResult = 4), OffsetTestData(offset = 1, 3))
            .forEach { testData ->
                test("given offset ${testData.offset} within range then return ${testData.firstResult} as first result") {
                    // Arrange
                    val offset = testData.offset
                    val limit = 1
                    // Act
                    val results = dataSet.pagQuery(offset, limit)
                    // Assert
                    results.shouldNotBeEmpty()
                    results.first().first() shouldBe testData.firstResult

                }
            }

        test("given offset out of range then return empty list") {
            // Arrange
            val offset = 10000
            val limit = 1
            // Act
            val results = dataSet.pagQuery(offset, limit)
            // Assert
            results.shouldBeEmpty()
        }

        test("given limit = 2 then return [[4,3], [1,7], [9,8], [5]]") {
            // Arrange
            val offset = 0
            val limit = 2
            // Act
            val results = dataSet.pagQuery(offset, limit)
            // Assert
            results.shouldNotBeEmpty()
            results.shouldHaveSize(4)
            results[0] shouldBe listOf(4, 3)
            results[1] shouldBe listOf(1, 7)
            results[2] shouldBe listOf(9, 8)
            results[3] shouldBe listOf(5)
        }
    }


})
