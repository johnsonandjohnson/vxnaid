package com.jnj.vaccinetracker.common.helpers

import encodeBase64ToString
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.random.Random


const val TEST_FILE="test.bin"

class FileExtKtTest : FunSpec({

    context("readBytesAsync") {
        test("when exists then return the bytes") {
            // Arrange
            val bytes = Random.nextBytes(100)
            val file = File(TEST_FILE)
            require(!file.exists())
            afterTest {
                file.delete()
            }
            file.writeBytes(bytes)
            // Act
            val result = file.readBytesAsync()
            // Assert
            result.encodeBase64ToString() shouldBe bytes.encodeBase64ToString()
        }

        test("when not exists then throw exception") {
            // Arrange
            val file = File(TEST_FILE)
            require(!file.exists())
            // Act
            try {
                file.readBytesAsync()
                // Assert
                fail("excepted exception")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    context("readBytesOrNull") {
        test("when exists then return the bytes") {
            // Arrange
            val bytes = Random.nextBytes(100)
            val file = File(TEST_FILE)
            require(!file.exists())
            afterTest {
                file.delete()
            }
            file.writeBytes(bytes)
            // Act
            val result = file.readBytesOrNull()
            // Assert
            result?.encodeBase64ToString() shouldBe bytes.encodeBase64ToString()
        }
        test("when not exists then return null") {
            // Arrange
            val file = File(TEST_FILE)
            require(!file.exists())
            // Act
            val result = file.readBytesOrNull()
            // Assert
            result.shouldBeNull()
        }
    }
})