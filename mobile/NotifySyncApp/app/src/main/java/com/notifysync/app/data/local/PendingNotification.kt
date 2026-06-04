package com.notifysync.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A captured notification awaiting upload. Survives process death and offline periods. */
@Entity(tableName = "pending_notifications")
data class PendingNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val title: String,
    val message: String,
    val notificationTime: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
)
