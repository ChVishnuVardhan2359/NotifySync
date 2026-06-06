package com.notifysync.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.DeviceDataRepository
import com.notifysync.app.data.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by [SyncScheduler] (works during Doze). Holds a short wake-lock, runs a full sync
 * (heartbeat, queue flush, current notifications, calls/SMS), then reschedules the next wake-up.
 */
class SyncAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        val pending = goAsync()

        val pm = appCtx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "notifysync:sync")
        wakeLock.acquire(60_000L) // safety cap

        val tokenStore = TokenStore(appCtx)
        val sync = SyncRepository(appCtx, tokenStore)
        val data = DeviceDataRepository(appCtx, tokenStore)

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (!tokenStore.token().isNullOrBlank() && tokenStore.syncEnabled()) {
                    sync.heartbeat()
                    NotifyListenerService.instance?.captureActiveNotifications()
                    sync.uploadPending()
                    if (sync.isSyncRequested()) sync.uploadPending()
                    data.syncCalls()
                    data.syncSms()
                }
            }
            // Always reschedule so the chain continues, even on error.
            SyncScheduler.schedule(appCtx)
            if (wakeLock.isHeld) wakeLock.release()
            pending.finish()
        }
    }
}
