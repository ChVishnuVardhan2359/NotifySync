package com.notifysync.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Schedules a repeating wake-up that fires even while the device is in Doze
 * (setAndAllowWhileIdle), so syncing keeps happening when the phone is untouched.
 */
object SyncScheduler {
    private const val REQUEST = 7001
    private const val INTERVAL_MS = 10 * 60 * 1000L // ~10 min (Doze floor is ~9 min for allowWhileIdle)

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SyncAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS
        // allowWhileIdle => fires during Doze; no special permission needed (inexact).
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }
}
