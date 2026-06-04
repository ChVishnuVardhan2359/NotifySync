package com.notifysync.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.notifysync.app.MainActivity
import com.notifysync.app.R
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.DeviceDataRepository
import com.notifysync.app.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent foreground service that keeps NotifySync alive in the background (even after the
 * app is swiped from recents / screen off). Every cycle it: sends a heartbeat (keeps the device
 * "online"), flushes the upload queue, and checks whether the dashboard requested a sync — if so
 * it captures the currently-visible notifications via the listener.
 */
class SyncForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tokenStore: TokenStore
    private lateinit var repo: SyncRepository
    private lateinit var deviceData: DeviceDataRepository
    private var cycle = 0

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(applicationContext)
        repo = SyncRepository(applicationContext, tokenStore)
        deviceData = DeviceDataRepository(applicationContext, tokenStore)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        scope.launch {
            while (isActive) {
                runCatching {
                    if (tokenStore.token().isNullOrBlank()) {
                        stopSelf()
                        return@launch
                    }
                    if (tokenStore.syncEnabled()) {
                        repo.heartbeat()
                        // Continuously capture whatever is on the status bar (dedupe prevents repeats),
                        // so notifications sync automatically with no button press.
                        NotifyListenerService.instance?.captureActiveNotifications()
                        repo.uploadPending()

                        // The dashboard's "Sync Now" sets a flag — when seen, pull everything now.
                        val requested = repo.isSyncRequested()
                        if (requested || cycle % CALLS_SMS_EVERY == 0) {
                            deviceData.syncCalls()
                            deviceData.syncSms()
                        }
                    }
                }
                cycle++
                delay(CYCLE_MS)
            }
        }
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sync_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.sync_channel_desc) }
            manager.createNotificationChannel(channel)
        }

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NotifySync is running")
            .setContentText("Syncing your notifications in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "notifysync_sync"
        private const val NOTIFICATION_ID = 1001
        private const val CYCLE_MS = 12_000L
        /** Auto-refresh call log + SMS every this many cycles (25 × 12s ≈ 5 min). */
        private const val CALLS_SMS_EVERY = 25

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SyncForegroundService::class.java),
            )
        }
    }
}
