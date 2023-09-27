package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher

class InternetConnectivityKtTest : FunSpec({

    test("test google") {
        val hasIp = InternetConnectivity.hasIpAddress("google.com")
        hasIp shouldBe true
    }

    test("test demo iris") {
        val hasIp = InternetConnectivity.hasIpAddress("demo-iris.jnj.connect-for-life.org")
        hasIp shouldBe true
    }

    test("test demo mdacs full with slash at end") {
        val hasIp = InternetConnectivity.hasIpAddress("https://demo-mdacs.jnj.connect-for-life.org/")
        hasIp shouldBe true
    }

    test("test demo iris full without slash") {
        val hasIp = InternetConnectivity.hasIpAddress("https://vxnaid-development.jnj.connect-for-life.org")
        hasIp shouldBe true
    }

    test("test demo iris full with slash at end") {
        val hasIp = InternetConnectivity.hasIpAddress("https://vxnaid-development.jnj.connect-for-life.org/")
        hasIp shouldBe true
    }

    test("observeInternetConnectivity") {
        // Arrange
        val dispatcher = TestCoroutineDispatcher()
        val dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(dispatcher)
        val syncSettingsRepo: SyncSettingsRepository = mockk()
        var backendUrl: String? = "invalid address"
        coEvery { syncSettingsRepo.getBackendUrlOrNull() } coAnswers {
            backendUrl
        }
        val internetConnectivity = InternetConnectivity(dispatchers = dispatchers, syncSettingsRepo)
        val result = MutableStateFlow(0)
        suspend fun delayShort() = delay(333)
        val job = launch {
            println("launched internet connectivity job")
            internetConnectivity.observeInternetConnectivity(100000).collect {
                println("observeInternetConnectivity: $it")
                result.value++
            }
        }
        delayShort()
        println("expecting job running")
        result.value shouldBe 1
        backendUrl = null
        internetConnectivity.isInternetConnected() shouldBe false
        internetConnectivity.isInternetConnectedAccurate(true)
        internetConnectivity.isInternetConnected() shouldBe true
        delayShort()
        result.value shouldBe 2
        job.cancel()
    }

})
