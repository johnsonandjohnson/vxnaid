package com.jnj.vaccinetracker.common.data.helpers

import com.jnj.vaccinetracker.common.di.NetworkModule
import com.jnj.vaccinetracker.common.domain.entities.Configuration
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestCoroutineDispatcher
import readResource

class Md5HashGeneratorTest : FunSpec({

    val sut = Md5HashGenerator(AppCoroutineDispatchers.fromSingleDispatcher(TestCoroutineDispatcher()))

    data class TestEntry(val fileName: String, val expectedHash: String)
    listOf(TestEntry(
        fileName = "locations.json",
        expectedHash = "5bf6310657e39663404bb5a74c82c5d8"
    ), TestEntry(
        fileName = "addresshierarchy.json",
        expectedHash = "723fd3d958c0c61c1fb3eb692ca41a3a"
    ), TestEntry(
        fileName = "localization.json",
        expectedHash = "8a5d28d7963d42ed1fe4ae9666f0de57"
    ), TestEntry(
        fileName = "configuration.json",
        expectedHash = "7296b1903dc4d062fbc964b6fc617319"
    )).forEach { testEntry ->
        test("given ${testEntry.fileName} then return md5 hash ${testEntry.expectedHash}") {
            val input = readResource(testEntry.fileName)
            val result = sut.md5(input)
            result shouldBe testEntry.expectedHash
        }
    }


    test("config test") {
        val input = readResource("configuration.json")
        val moshi = NetworkModule().provideMoshi()
        val adapter = moshi.adapter(Configuration::class.java)
        val config = adapter.fromJson(input)
        val output = adapter.toJson(config)!!
        println(output)
        val backendHash = "79a7cd1ee6d4da38ef04db2e9aa79974"
        val localHash = sut.md5(output)
        localHash shouldBe backendHash
    }

})
