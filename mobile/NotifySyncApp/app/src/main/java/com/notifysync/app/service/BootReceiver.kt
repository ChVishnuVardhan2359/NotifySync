package com.notifysync.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifysync.app.worker.UploadWorker

/** Resumes the background sync service and queue after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            UploadWorker.enqueue(context.applicationContext)
            SyncForegroundService.start(context.applicationContext)
        }
    }
}
