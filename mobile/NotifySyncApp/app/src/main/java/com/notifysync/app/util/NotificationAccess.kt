package com.notifysync.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.notifysync.app.service.NotifyListenerService

object NotificationAccess {

    /** True if the user has granted Notification Access to our listener service. */
    fun isGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(context, NotifyListenerService::class.java)
        return flat.split(":").any {
            val cn = ComponentName.unflattenFromString(it)
            cn != null && cn == component
        }
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
