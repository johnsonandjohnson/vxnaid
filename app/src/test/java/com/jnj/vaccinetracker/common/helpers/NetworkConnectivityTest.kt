package com.jnj.vaccinetracker.common.helpers

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.*

class NetworkConnectivityTest : FunSpec({

    val testDispatcher = TestCoroutineDispatcher()
    val reactiveNetworkConnectivity: ReactiveNetworkConnectivity = mockk()
    val internetConnectivity: InternetConnectivity = mockk()
    val connectivity = MutableStateFlow(Connectivity.disconnected())
    val serverHealthMeter: ServerHealthMeter = mockk()
    every { reactiveNetworkConnectivity.observeNetworkConnectivity() } returns connectivity
    every { internetConnectivity.observeInternetConnectivity() } returns flowOf(true)
    val networkConnectivity =
        NetworkConnectivityDefault(AppCoroutineDispatchers.fromSingleDispatcher(testDispatcher), reactiveNetworkConnectivity, internetConnectivity, serverHealthMeter)

    test("when internet is not available then wait otherwise return") {
        val isDoneWaiting = MutableStateFlow(false)
        launch {
            assertFalse(networkConnectivity.isConnectedFast())
            println("waiting")
            try {
                networkConnectivity.awaitFastInternet("test")
            } catch (ex: Throwable) {
                fail("did not expect exception: $ex")
            }
            println("done waiting")
            assertTrue(networkConnectivity.isConnectedFast())
            isDoneWaiting.value = true
        }
        delay(100)
        connectivity.emit(Connectivity.disconnected())
        assertFalse(isDoneWaiting.value)
        delay(100)
        connectivity.emit(Connectivity(Connectivity.Type.WIFI, false))
        delay(100)
        assertTrue(isDoneWaiting.value)
    }
})