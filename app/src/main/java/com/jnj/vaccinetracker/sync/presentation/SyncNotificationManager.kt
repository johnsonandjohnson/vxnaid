package com.jnj.vaccinetracker.sync.presentation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.format
import com.jnj.vaccinetracker.sync.domain.entities.SyncState
import com.jnj.vaccinetracker.sync.presentation.base.ForegroundService
import javax.inject.Inject


class SyncNotificationManager @Inject constructor(private val context: Context) {

    fun onCreate(foregroundService: ForegroundService, syncState: SyncState = SyncState.Idle) {
        createNotification(foregroundService, syncState)
    }

    fun updateNotification(foregroundService: ForegroundService, syncState: SyncState) {
        createNotification(foregroundService, syncState)
    }

    private fun createNotification(
        foregroundService: ForegroundService,
        syncState: SyncState,
    ) {
        logInfo("createNotification $syncState")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = getNotification(syncState)

        if (isNotificationVisible(context))
            logInfo("updating notification")

        foregroundService.startForeground(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL,
                context.getString(R.string.sync_service_name),
                NotificationManager.IMPORTANCE_NONE
            )
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
    }

    private fun SyncState.toLocalizedString(): String {
        return when (this) {
            is SyncState.Idle -> context.getString(R.string.sync_status_idle)
            SyncState.OnlineInSync -> context.getString(R.string.sync_status_online_in_sync)
            SyncState.OnlineSyncing ->
                context.getString(R.string.sync_status_syncing)
            is SyncState.Offline ->
                context.getString(R.string.sync_status_offline, lastSyncDate.format())
            is SyncState.OfflineOutOfSync ->
                context.getString(R.string.sync_status_offline, lastSyncDate?.format() ?: context.getString(R.string.general_label_na))
            is SyncState.SyncError ->
                context.getString(R.string.sync_status_sync_error_no_tap)
            is SyncState.SyncComplete -> context.getString(R.string.sync_status_complete, lastSyncDate.format())
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createContentIntent(): PendingIntent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, Intent(),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, Intent(),
                PendingIntent.FLAG_ONE_SHOT)
        }
    }

    private fun getNotification(syncState: SyncState): Notification {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL
        )
        val contentText = syncState.toLocalizedString()
        val contentIntent = createContentIntent()
        return builder.setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(
                context.resources.getString(R.string.sync_service_notification_title)
            )
            .setContentIntent(contentIntent)
            .setContentText(contentText)
            .setOngoing(true)
            .setChannelId(NOTIFICATION_CHANNEL)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 123321
        private const val NOTIFICATION_CHANNEL = "VaccineTrackerSyncNotificationManager"
        fun isNotificationVisible(context: Context): Boolean {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mgr.activeNotifications.any { it.id == NOTIFICATION_ID }
            } else false
        }
    }
}