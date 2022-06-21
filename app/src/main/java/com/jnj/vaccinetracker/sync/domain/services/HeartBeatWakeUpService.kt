package com.jnj.vaccinetracker.sync.domain.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jnj.vaccinetracker.common.helpers.seconds
import com.jnj.vaccinetracker.sync.presentation.SyncAndroidService
import javax.inject.Inject

class HeartBeatWakeUpService @Inject constructor(val context: Context) {

    companion object {
        private const val HEART_BEAT_REQUEST_CODE = 12345
        private val triggerInterval = 10.seconds
        private val triggerStart = System.currentTimeMillis() + triggerInterval
    }

    fun startWakeUpHeartBeat() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // set alarm to upload the events
        val i = Intent(context, SyncAndroidService::class.java)
        val pi = PendingIntent.getService(context, HEART_BEAT_REQUEST_CODE, i, PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerStart, triggerInterval, pi)
    }

}