package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.entities.DateModifiedOccurrence
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import com.jnj.vaccinetracker.sync.domain.services.ParticipantDataDownstreamSyncService.Companion.shouldOptimize
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

class ParticipantDataDownstreamSyncServiceKtTest : FunSpec({


    test("findEntityTypesBySuccess") {
        // Arrange
        val syncResults: SyncResults = mutableMapOf()
        syncResults[1] = mapOf(SyncEntityType.IMAGE to SyncCompleted(true), SyncEntityType.PARTICIPANT to SyncCompleted(false))
        syncResults[2] = mapOf(SyncEntityType.PARTICIPANT to SyncCompleted(true))
        syncResults[3] = mapOf(SyncEntityType.BIOMETRICS_TEMPLATE to SyncCompleted(true), SyncEntityType.VISIT to SyncCompleted(true))
        // Act & Assert
        syncResults.findEntityTypesBySuccess(true) shouldContainAll SyncEntityType.values().toList()
        syncResults.findEntityTypesBySuccess(false) shouldContainAll listOf(SyncEntityType.PARTICIPANT)
    }

    test("calcSyncDate") {
        // Arrange
        val syncResults: SyncResults = mutableMapOf()
        syncResults[1] = mapOf(SyncEntityType.IMAGE to SyncCompleted(true, SyncDate(6)), SyncEntityType.PARTICIPANT to SyncCompleted(false, SyncDate(4)))
        syncResults[2] = mapOf(SyncEntityType.PARTICIPANT to SyncCompleted(true, SyncDate(5)))
        syncResults[3] = mapOf(SyncEntityType.BIOMETRICS_TEMPLATE to SyncCompleted(true), SyncEntityType.VISIT to SyncCompleted(true))
        // Act
        val syncDate = syncResults.calcSyncDate()
        // Assert
        syncDate shouldBe SyncDate(5)
    }

    context("shouldOptimize") {
        val futureDate = DateEntity(dateNow().time + 100.days)
        fun createSyncScope(dateCreated: DateEntity) = SyncScope(country = "India", siteUuid = "site", cluster = null, level = SyncScopeLevel.COUNTRY, dateCreated = dateCreated)
        context("given dateModified is null") {
            test("then return false for all entity types") {
                val types = SyncEntityType.values()
                types.forEach { set ->
                    set.shouldOptimize(createSyncScope(dateNow()), null).shouldBeFalse()
                }
            }
        }
        context("given date modified not null") {
            test("given participant or visit then return false always") {
                val types = listOf(SyncEntityType.PARTICIPANT, SyncEntityType.VISIT)
                types.forEach { set ->
                    set.shouldOptimize(createSyncScope(dateNow()),
                        DateModifiedOccurrence(futureDate, listOf("uuid"))).shouldBeFalse()
                }
            }

            test("given biometric or image then return true when date modified minus safety date offset is more recent than sync scope date") {
                val types = listOf(SyncEntityType.IMAGE, SyncEntityType.BIOMETRICS_TEMPLATE)
                types.forEach { set ->
                    val safetyDateOffset = 10L
                    fun dateModifiedOccurrence(dateModified: Long) = DateModifiedOccurrence(DateEntity(dateModified), listOf("uuid"))
                    set.shouldOptimize(createSyncScope(DateEntity(90)), dateModifiedOccurrence(100), safetyDateOffset).shouldBeFalse()
                    set.shouldOptimize(createSyncScope(DateEntity(89)), dateModifiedOccurrence(100), safetyDateOffset).shouldBeTrue()
                }
            }
        }

    }
})
