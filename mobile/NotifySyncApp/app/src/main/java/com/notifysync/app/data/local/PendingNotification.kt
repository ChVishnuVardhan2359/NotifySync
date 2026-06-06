package com.notifysync.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A captured notification persisted immediately to the offline queue. Survives process death,
 * reboots, and network loss. [eventKey] is unique so the same notification is never queued twice,
 * and [status] tracks its lifecycle: pending -> syncing -> synced | failed.
 */
@Entity(
    tableName = "pending_notifications",
    indices = [Index(value = ["eventKey"], unique = true)],
)
data class PendingNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventKey: String,
    val appName: String,
    val packageName: String,
    val title: String,
    val message: String,
    val notificationTime: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SYNCING = "syncing"
        const val STATUS_SYNCED = "synced"
        const val STATUS_FAILED = "failed"
    }
}
