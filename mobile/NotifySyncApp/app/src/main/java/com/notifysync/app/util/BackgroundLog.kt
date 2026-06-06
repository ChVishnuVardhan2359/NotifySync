package com.notifysync.app.util

import android.util.Log
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Lightweight logger for background events (service start/stop, sync attempts, failures,
 * recoveries, boot, network changes). Writes to Logcat and keeps a small in-memory ring
 * buffer that the UI can surface for diagnostics.
 */
object BackgroundLog {
    private const val TAG = "NotifySyncBG"
    private const val MAX = 200
    private val buffer = ConcurrentLinkedDeque<String>()

    fun event(message: String) = add("EVENT", message)
    fun sync(message: String) = add("SYNC", message)
    fun warn(message: String) = add("WARN", message)
    fun error(message: String, t: Throwable? = null) {
        add("ERROR", message)
        if (t != null) Log.e(TAG, message, t)
    }

    private fun add(level: String, message: String) {
        val line = "${nowIso()} [$level] $message"
        Log.i(TAG, "$level: $message")
        buffer.addFirst(line)
        while (buffer.size > MAX) buffer.pollLast()
    }

    fun recent(): List<String> = buffer.toList()

    private fun nowIso(): String = runCatching { Instant.now().toString() }.getOrDefault("")
}
