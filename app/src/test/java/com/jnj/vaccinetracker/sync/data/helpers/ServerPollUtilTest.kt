package com.jnj.vaccinetracker.sync.data.helpers

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.sync.domain.helpers.ForceSyncObserver
import com.jnj.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher

class ServerPollUtilTest : FunSpec({
    val syncSettingsObserver: SyncSettingsObserver = mockk()
    val networkConnectivity: NetworkConnectivity = mockk()
    val forceSyncObserver: ForceSyncObserver = mockk()
    val dispatcher = TestCoroutineDispatcher()

    val serverPoll = ServerPollUtil(syncSettingsObserver, networkConnectivity, forceSyncObserver, AppCoroutineDispatchers.fromSingleDispatcher(dispatcher))
    test("skipDelayWhenSyncSettingsChanged") {
        //Arrange
        val siteSelectionChanged = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
        siteSelectionChanged.tryEmit("test")
        val loopCount = MutableStateFlow(0)
        val loopDone = MutableStateFlow(false)
        coEvery { networkConnectivity.awaitFastInternet(any()) } returns Unit
        coEvery { networkConnectivity.isConnectedFast() } returns true
        coEvery { forceSyncObserver.observeForceSync() } returns emptyFlow()
        coEvery { syncSettingsObserver.isSyncSettingsAvailable() } returns true
        coEvery { syncSettingsObserver.isSiteSelectionAvailable() } returns true
        coEvery { syncSettingsObserver.awaitSyncSettingsAvailable(any()) } returns Unit
        coEvery { syncSettingsObserver.observeSiteSelectionChanges() } coAnswers {
            siteSelectionChanged
        }
        // Act
        loopCount.value shouldBe 0
        launch {
            serverPoll.pollServerPeriodically(1000, "test") {
                loopCount.value++
                !loopDone.value
            }
        }
        delay(50)
        loopCount.value shouldBe 1
        delay(50)
        loopCount.value shouldBe 1
        siteSelectionChanged.emit("test2")
        delay(50)
        loopCount.value shouldBe 2
        println("$loopCount")
        delay(50)
        loopDone.emit(true)
    }
})
