package com.jnj.vaccinetracker.sync.presentation.base

import android.app.Notification

interface ForegroundService {
    fun stopForeground(removeNotification: Boolean)
    fun startForeground(id: Int, notification: Notification)
    fun stopSelf()
}