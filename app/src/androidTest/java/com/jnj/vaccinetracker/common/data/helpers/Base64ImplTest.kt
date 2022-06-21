package com.jnj.vaccinetracker.common.data.helpers

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class Base64ImplTest {

    private val base64 = Base64Impl(AppCoroutineDispatchers.DEFAULT)

    @Test
    fun outPutShouldBeCompatibleWithDefaultAndroidImpl(): Unit = runBlocking {
        repeat(10) {
            val bytes = Random.nextBytes(100)
            val currentValue = base64.encode(bytes)
            val androidValue = Base64.encodeToString(bytes, Base64.DEFAULT)
            base64.decode(currentValue) shouldBe base64.decode(androidValue)
        }
    }

    @Test
    fun outPutShouldBeCompatibleWithCommonKotlinImpl() = runBlocking {
        repeat(10) {
            val bytes = Random.nextBytes(100)
            val currentValue = base64.encode(bytes)
            val kotlinValue = bytes.toByteString().base64()
            base64.decode(currentValue) shouldBe base64.decode(kotlinValue)
        }
    }

    @Test
    fun kotlinImplIsCompatibleWithAndroidImpl() = runBlocking {
        repeat(10) {
            val bytes = Random.nextBytes(100)
            val androidValue = Base64.encodeToString(bytes, Base64.DEFAULT)
            val kotlinValue = bytes.toByteString().base64()
            kotlinValue.decodeBase64()!!.toByteArray() shouldBe Base64.decode(androidValue, Base64.DEFAULT)
        }
    }

    @Test
    fun testEmptyAndroidImpl(): Unit = runBlocking {
        val bytes = byteArrayOf()
        val result = Base64.encodeToString(bytes, Base64.DEFAULT)
        println("result: $result")
        result.shouldBeEmpty()
        Base64.decode(result, Base64.DEFAULT) shouldBe byteArrayOf()
    }

    @Test
    fun testEmptyKotlinImpl(): Unit = runBlocking {
        val bytes = byteArrayOf()
        val result = bytes.toByteString().base64()
        println("result: $result")
        result.shouldBeEmpty()
        result.decodeBase64()?.toByteArray() shouldBe byteArrayOf()
    }

    @Test
    fun testEmptyCurrentImpl(): Unit = runBlocking {
        val bytes = byteArrayOf()
        val result = base64.encode(bytes)
        println("result: $result")
        result.shouldBeEmpty()
        base64.decode(result) shouldBe byteArrayOf()
    }
}
