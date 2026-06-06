package com.notifysync.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.util.BackgroundLog
import com.notifysync.app.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores all background components after a reboot (or app update): the foreground service,
 * the periodic recovery worker, the Doze alarm, the connectivity monitor, and an immediate
 * flush of any unsynced data.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val appCtx = context.applicationContext
        BackgroundLog.event("boot/update received ($action) — restoring background components")

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val tokenStore = TokenStore(appCtx)
                // Always restore the scheduled safety nets.
                UploadWorker.schedulePeriodic(appCtx)
                SyncScheduler.schedule(appCtx)
                ConnectivityObserver.start(appCtx)
                UploadWorker.enqueue(appCtx) // resume uploading unsynced data

                // Bring the foreground service back if the user is signed in.
                if (!tokenStore.token().isNullOrBlank()) {
                    SyncForegroundService.start(appCtx)
                }
            }
            pending.finish()
        }
    }
}
