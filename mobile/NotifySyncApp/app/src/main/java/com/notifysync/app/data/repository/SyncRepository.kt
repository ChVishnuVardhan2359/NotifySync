package com.notifysync.app.data.repository

import android.content.Context
import com.notifysync.app.data.api.CreateNotificationRequest
import com.notifysync.app.data.api.HeartbeatRequest
import com.notifysync.app.data.api.NotificationDto
import com.notifysync.app.data.api.RetrofitProvider
import com.notifysync.app.data.local.AppDatabase
import com.notifysync.app.data.local.PendingNotification
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.util.DeviceInfo

/**
 * Owns the offline queue and its upload. Captured notifications are persisted first
 * (so nothing is lost offline) and uploaded by the WorkManager job with retry.
 */
class SyncRepository(
    private val context: Context,
    private val tokenStore: TokenStore,
) {
    private val dao = AppDatabase.get(context).notificationDao()
    private val api get() = RetrofitProvider.api(tokenStore)

    val pendingCount = dao.pendingCount()

    suspend fun enqueue(notification: PendingNotification) {
        dao.insert(notification)
    }

    /** Uploads queued notifications. Returns true if the queue drained cleanly. */
    suspend fun uploadPending(): Boolean {
        val deviceId = tokenStore.deviceId() ?: return false
        if (tokenStore.token().isNullOrBlank()) return false

        var allOk = true
        while (true) {
            val batch = dao.nextBatch(BATCH_SIZE)
            if (batch.isEmpty()) break

            for (item in batch) {
                val ok = runCatching {
                    val res = api.createNotification(
                        CreateNotificationRequest(
                            deviceId = deviceId,
                            appName = item.appName,
                            packageName = item.packageName,
                            title = item.title,
                            message = item.message,
                            notificationTime = item.notificationTime,
                        ),
                    )
                    // 2xx => stored; 409 => sync disabled server-side, drop it to avoid looping.
                    res.isSuccessful || res.code() == 409
                }.getOrDefault(false)

                if (ok) {
                    dao.delete(item.id)
                } else {
                    dao.incrementAttempts(item.id)
                    allOk = false
                    return allOk // stop on first hard failure; Worker will retry later
                }
            }
        }
        return allOk
    }

    suspend fun heartbeat() {
        val identifier = DeviceInfo.deviceIdentifier(context)
        runCatching { api.heartbeat(HeartbeatRequest(identifier)) }
    }

    /** The notifications this account has synced to the server (most recent first). */
    suspend fun recentNotifications(): List<NotificationDto> {
        if (tokenStore.token().isNullOrBlank()) return emptyList()
        return runCatching {
            api.getNotifications(page = 1, pageSize = 100).body()?.items ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /** Total notifications stored on the server, or null if unreachable (i.e. not connected). */
    suspend fun notificationTotal(): Int? {
        if (tokenStore.token().isNullOrBlank()) return null
        return runCatching { api.getNotifications(1, 1).body()?.totalCount }.getOrNull()
    }

    /** Returns true if the dashboard pressed "Sync Now" for this device (consumes the flag). */
    suspend fun isSyncRequested(): Boolean {
        if (tokenStore.token().isNullOrBlank()) return false
        val identifier = DeviceInfo.deviceIdentifier(context)
        return runCatching {
            api.syncPending(identifier).body()?.pending == true
        }.getOrDefault(false)
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}
