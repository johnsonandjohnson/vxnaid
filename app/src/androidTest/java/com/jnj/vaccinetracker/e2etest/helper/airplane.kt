package com.jnj.vaccinetracker.e2etest.helper

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun ActivityScenarioRule<out AppCompatActivity>.awaitActivityResumed() = suspendCoroutine<Unit> { continuation ->
    scenario.onActivity() { activity ->
        var observer: LifecycleObserver? = null
        observer = LifecycleEventObserver { source, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activity.lifecycle.removeObserver(observer!!)
                continuation.resume(Unit)
            }
        }
        activity.lifecycle.addObserver(observer)
    }
}

fun showMessage(message: String) = runBlocking(Dispatchers.Main.immediate) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

suspend fun ActivityScenarioRule<out AppCompatActivity>.awaitAirplaneModeSettings(isAirplaneModeRequired: Boolean) {
    if (isAirplaneModeRequired != isAirplaneModeOn()) {
        val message = if (isAirplaneModeRequired) {
            "Please enable airplane mode"
        } else "Please disable airplane mode"
        showMessage(message)
        context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        awaitActivityResumed()
        awaitAirplaneModeSettings(isAirplaneModeRequired)
        delay(3000)
    }
}


fun isAirplaneModeOn(): Boolean {
    return Settings.Global.getInt(context.contentResolver,
        Settings.Global.AIRPLANE_MODE_ON, 0) != 0
}