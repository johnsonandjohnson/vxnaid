package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.sync.domain.entities.SyncPageProgress
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ReadyForMigrationSignalerTest : FunSpec({
    val syncLogger: SyncLogger = mockk()
    val readyForMigrationSignaler = ReadyForMigrationSignaler(syncLogger)
    test("awaitReady") {
        // Arrange
        val syncPageProgressMap = SyncEntityType.values().map { it to MutableStateFlow(SyncPageProgress.DOWNLOADING_PAGE) }.toMap()
        for ((key, value) in syncPageProgressMap.entries) {
            coEvery { syncLogger.observeSyncPageProgress(key) } returns value
            every { syncLogger.getSyncPageProgress(key) } answers {
                value.value
            }
        }
        val isReady = MutableStateFlow(false)
        // Act
        launch {
            readyForMigrationSignaler.awaitReady()
            println("isReady=true")
            isReady.value = true
        }
        // Assert
        val delayMs = 100L
        delay(delayMs)
        isReady.value shouldBe false
        syncPageProgressMap[SyncEntityType.BIOMETRICS_TEMPLATE]!!.value = SyncPageProgress.IDLE
        delay(delayMs)
        isReady.value shouldBe false
        syncPageProgressMap[SyncEntityType.IMAGE]!!.value = SyncPageProgress.BUILDING_SYNC_REQUEST
        delay(delayMs)
        isReady.value shouldBe false
        syncPageProgressMap[SyncEntityType.PARTICIPANT]!!.value = SyncPageProgress.IDLE
        delay(delayMs)
        isReady.value shouldBe false
        syncPageProgressMap[SyncEntityType.PARTICIPANT]!!.value = SyncPageProgress.DOWNLOADING_PAGE
        syncPageProgressMap[SyncEntityType.VISIT]!!.value = SyncPageProgress.BUILDING_SYNC_REQUEST
        delay(delayMs)
        isReady.value shouldBe false
        syncPageProgressMap[SyncEntityType.PARTICIPANT]!!.value = SyncPageProgress.BUILDING_SYNC_REQUEST
        delay(delayMs)
        isReady.value shouldBe true
    }
})

