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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.notifysync.app.MainActivity
import com.notifysync.app.R
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.DeviceDataRepository
import com.notifysync.app.data.repository.SyncRepository
import com.notifysync.app.util.BackgroundLog
import com.notifysync.app.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent foreground service that keeps NotifySync alive in the background. It holds a partial
 * wake-lock so its ~2.5-minute loop keeps running even when the screen is off (so the device stays
 * "online"), and layers on event-driven + scheduled safety nets (connectivity callback, periodic
 * WorkManager, Doze alarm) so nothing is lost if the service is ever killed.
 */
class SyncForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tokenStore: TokenStore
    private lateinit var repo: SyncRepository
    private lateinit var deviceData: DeviceDataRepository
    private var wakeLock: PowerManager.WakeLock? = null
    private var cycle = 0

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(applicationContext)
        repo = SyncRepository(applicationContext, tokenStore)
        deviceData = DeviceDataRepository(applicationContext, tokenStore)
        startForegroundNotification()
        acquireWakeLock()

        // Layered reliability: event-driven (network) + scheduled (WorkManager + Doze alarm).
        ConnectivityObserver.start(applicationContext)
        UploadWorker.schedulePeriodic(applicationContext)
        SyncScheduler.schedule(applicationContext)
        BackgroundLog.event("foreground service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLoop()
        return START_STICKY // recreate the service if the system kills it
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
                        NotifyListenerService.instance?.captureActiveNotifications()
                        repo.uploadPending()

                        val requested = repo.isSyncRequested()
                        if (requested || cycle % CALLS_SMS_EVERY == 0) {
                            deviceData.syncCalls()
                            deviceData.syncSms()
                        }
                    }
                }.onFailure { BackgroundLog.error("loop cycle failed", it) }
                cycle++
                delay(CYCLE_MS)
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "notifysync:foreground").apply {
            setReferenceCounted(false)
            runCatching { acquire() }
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
            .setContentText("Keeping your notifications synced")
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

    /** Swiped from recents — keep the scheduled/persistent components alive. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        BackgroundLog.event("task removed — rescheduling background work")
        UploadWorker.schedulePeriodic(applicationContext)
        SyncScheduler.schedule(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        BackgroundLog.event("foreground service destroyed")
        scope.cancel()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        ConnectivityObserver.stop(applicationContext)
        // Make sure recovery still happens even though the live service is gone.
        SyncScheduler.schedule(applicationContext)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "notifysync_sync"
        private const val NOTIFICATION_ID = 1001
        private const val CYCLE_MS = 150_000L // ~2.5 min (kept running by the wake-lock)
        /** Refresh call log + SMS every this many cycles (2 × 2.5 min ≈ 5 min). */
        private const val CALLS_SMS_EVERY = 2

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SyncForegroundService::class.java),
            )
        }
    }
}
