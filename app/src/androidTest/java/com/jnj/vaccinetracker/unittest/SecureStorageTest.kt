package com.jnj.vaccinetracker.unittest

import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jnj.vaccinetracker.common.data.encryption.EncryptedSharedPreferencesFactory
import com.jnj.vaccinetracker.common.data.encryption.SecureStorageThreadSafe
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import de.codecentric.androidtestktx.common.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStorageTest {

    @Test
    fun testConcurrentModification(): Unit = runBlocking {
        val context = appContext
        val dispatchers = AppCoroutineDispatchers.DEFAULT
        val masterKey = MasterKey.Builder(context, "test")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .build()
        val prefsFactory = EncryptedSharedPreferencesFactory(context, masterKey)
        val prefs = SecureStorageThreadSafe("test", prefsFactory, dispatchers)
        launch(Dispatchers.IO) {
            repeat(1000) { i ->
                prefs.putString("test", "test$i")
                prefs.putString("test2$i", "test$i")
            }
        }
        launch(Dispatchers.IO) {
            repeat(1000) { i ->
                prefs.remove("test")
            }
        }

        val job = launch(Dispatchers.IO) {
            repeat(100) {
                delay(100)
                launch(Dispatchers.IO) {
                    prefs.observeString("test").collect {

                    }
                }
            }
        }
        launch {
            delay(10000)
            job.cancel()
        }
    }
}