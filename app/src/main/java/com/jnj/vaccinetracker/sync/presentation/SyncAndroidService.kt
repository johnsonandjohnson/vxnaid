package com.jnj.vaccinetracker.sync.presentation

import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.presentation.base.ForegroundService
import dagger.android.DaggerService
import javax.inject.Inject

class SyncAndroidService : DaggerService(), ForegroundService {


    @Inject
    lateinit var controller: SyncController

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        controller.onCreate(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        controller.onDestroy(this)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            logInfo("start sync android service")
            ContextCompat.startForegroundService(context, Intent(context, SyncAndroidService::class.java))
        }
    }
}