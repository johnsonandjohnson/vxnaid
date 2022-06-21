package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import java.util.*

class SyncScopeTest : FunSpec({
    fun syncScope(siteUuid: String, level: SyncScopeLevel, country: String? = null, cluster: String? = null, dateCreated: Date = dateNow()) =
        SyncScope(siteUuid, level, country, cluster, dateCreated)

    data class TestData(val existingSyncScope: SyncScope, val other: SyncScope, val description: String, val expectWithinBounds: Boolean)

    lateinit var level: SyncScopeLevel
    fun differentCountry(expectWithinBounds: Boolean) = TestData(
        existingSyncScope = syncScope("1", level, "Country A", "cluster B"),
        other = syncScope("1", level, "Country B", "cluster B"),
        description = "different country",
        expectWithinBounds = expectWithinBounds,
    )

    fun differentCluster(expectWithinBounds: Boolean) = TestData(
        existingSyncScope = syncScope("1", level, "Country A", "cluster B"),
        other = syncScope("1", level, "Country A", "cluster C"),
        description = "different cluster",
        expectWithinBounds = expectWithinBounds,
    )

    fun differentCountryCase(expectWithinBounds: Boolean) = TestData(
        existingSyncScope = syncScope("1", level, "Country A", "cluster B"),
        other = syncScope("1", level, "Country a", "cluster B"),
        description = "different country case",
        expectWithinBounds = expectWithinBounds,
    )

    fun differentClusterCase(expectWithinBounds: Boolean) = TestData(
        existingSyncScope = syncScope("1", level, "Country A", "cluster B"),
        other = syncScope("1", level, "Country A", "cluster b"),
        description = "different cluster case",
        expectWithinBounds = expectWithinBounds,
    )

    fun differentSiteCase(expectWithinBounds: Boolean) = TestData(
        existingSyncScope = syncScope("A", level, "Country A", "cluster B"),
        other = syncScope("a", level, "Country A", "cluster B"),
        description = "different siteUuid case",
        expectWithinBounds = expectWithinBounds,
    )

    fun differentSiteUuid(expectWithinBounds: Boolean) = TestData(
        existingSyncScope = syncScope("1", level, "Country A", "cluster B"),
        other = syncScope("2", level, "Country A", "cluster B"),
        description = "different siteUuid",
        expectWithinBounds = expectWithinBounds,
    )

    suspend fun FunSpecContainerContext.runTestData(testData: TestData) {
        with(testData) {
            test("and $description then return $expectWithinBounds") {
                val result = existingSyncScope.isWithinBounds(other)
                result shouldBe expectWithinBounds
            }
        }
    }
    context("syncScope.isWithinBounds and levels are equal") {
        context("and data is different ignore case") {
            mapOf(SyncScopeLevel.COUNTRY to {
                listOf(
                    differentCountry(false),
                    differentCluster(true),
                    differentSiteUuid(true),
                    differentCountryCase(true),
                    differentClusterCase(true),
                    differentSiteCase(true)
                )
            }, SyncScopeLevel.CLUSTER to {
                listOf(
                    differentCountry(false),
                    differentCluster(false),
                    differentSiteUuid(true),
                    differentCountryCase(true),
                    differentClusterCase(true),
                    differentSiteCase(true)
                )
            }, SyncScopeLevel.SITE to {
                listOf(
                    differentCountry(true),
                    differentCluster(true),
                    differentSiteUuid(false),
                    differentCountryCase(true),
                    differentClusterCase(true),
                    differentSiteCase(false)
                )
            }).forEach { (syncScopeLevel, testDataList) ->
                context("and level = $syncScopeLevel") {
                    level = syncScopeLevel
                    testDataList().forEach { runTestData(it) }
                }
            }
        }
    }

})
