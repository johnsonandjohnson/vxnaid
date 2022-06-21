/*
 * Copyright (C) 2017 Piotr Wittchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnj.vaccinetracker.common.helpers

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject


class ReactiveAirplaneMode @Inject constructor(val dispatchers: AppCoroutineDispatchers) {
    /**
     * Emits current state of the Airplane Mode in the beginning of the subscription
     * and then Observes Airplane Mode state of the device with BroadcastReceiver.
     * RxJava2 Observable emits true if the airplane mode turns on and false otherwise.
     *
     * @param context of the Application or Activity
     * @return RxJava2 Observable with Boolean value indicating state of the airplane mode
     */
    fun getAndObserve(context: Context): Flow<Boolean> {
        return observe(context).onStart { emit(isAirplaneModeOn(context)) }
    }

    /**
     * Observes Airplane Mode state of the device with BroadcastReceiver.
     * RxJava2 Observable emits true if the airplane mode turns on and false otherwise.
     *
     * @param context of the Application or Activity
     * @return RxJava2 Observable with Boolean value indicating state of the airplane mode
     */
    fun observe(context: Context): Flow<Boolean> {
        val filter = createIntentFilter()
        return channelFlow {
            val receiver = createBroadcastReceiver(this)
            context.registerReceiver(receiver, filter)
            awaitClose {
                tryToUnregisterReceiver(receiver, context)
            }
        }
    }

    /**
     * Creates IntentFilter for BroadcastReceiver
     *
     * @return IntentFilter
     */
    protected fun createIntentFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        return filter
    }

    /**
     * Creates BroadcastReceiver for monitoring airplane mode
     *
     * @param emitter for RxJava
     * @return BroadcastReceiver
     */
    protected fun CoroutineScope.createBroadcastReceiver(
        emitter: ProducerScope<Boolean>
    ): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isAirplaneModeOn = intent.getBooleanExtra(INTENT_EXTRA_STATE, false)
                launch {
                    emitter.send(isAirplaneModeOn)
                }
            }
        }
    }

    /**
     * Tries to unregister BroadcastReceiver.
     * Calls [.onError] method
     * if receiver was already unregistered
     *
     * @param receiver BroadcastReceiver
     * @param context  of the Application or Activity
     */
    protected fun CoroutineScope.tryToUnregisterReceiver(receiver: BroadcastReceiver?, context: Context) {
        logInfo("tryToUnregisterReceiver")
        launch(dispatchers.main) {
            try {
                context.unregisterReceiver(receiver)
            } catch (exception: Exception) {
                /**
                 * Handles errors which occurs within this class
                 *
                 * @param message   with an error
                 * @param exception which occurred
                 */
                logError("receiver was already unregistered", exception)
            }
        }

    }

    /**
     * Checks airplane mode once basing on the system settings.
     * Returns true if airplane mode is on or false otherwise.
     *
     * @param context of the Activity or Application
     * @return boolean value indicating state of the airplane mode.
     */
    fun isAirplaneModeOn(context: Context): Boolean {
        val airplaneModeOnSetting: String = if (isAtLeastAndroidJellyBeanMr1) {
            airplaneModeOnSettingGlobal
        } else {
            airplaneModeOnSettingSystem
        }
        return Settings.System.getInt(context.contentResolver, airplaneModeOnSetting, 0) == 1
    }

    /**
     * Returns setting indicating airplane mode (for Android 17 and higher)
     *
     * @return String indicating airplane mode setting
     */
    @get:TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private val airplaneModeOnSettingGlobal: String
        private get() = Settings.Global.AIRPLANE_MODE_ON

    /**
     * Returns setting indicating airplane mode (for Android 16 and lower)
     *
     * @return String indicating airplane mode setting
     */
    private val airplaneModeOnSettingSystem: String
        private get() = Settings.System.AIRPLANE_MODE_ON

    /**
     * Validation method, which checks if context of the Activity or Application is not null
     *
     * @param context of the Activity or application
     */
    protected fun checkContextIsNotNull(context: Context?) {
        requireNotNull(context) { "context == null" }
    }

    /**
     * Validation method, which checks if current Android version is at least Jelly Bean MR1 (API 17)
     * or higher
     *
     * @return boolean true if current Android version is Jelly Bean MR1 or higher
     */
    private val isAtLeastAndroidJellyBeanMr1: Boolean
        private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1

    companion object {
        protected const val INTENT_EXTRA_STATE = "state"
    }
}