package com.notifysync.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceInfo {

    fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    @SuppressLint("HardwareIds")
    fun deviceIdentifier(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        )
        return if (androidId.isNullOrBlank()) "ns-${Build.FINGERPRINT.hashCode()}" else androidId
    }
}
