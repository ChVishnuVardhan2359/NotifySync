package com.notifysync.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: PendingNotification): Long

    @Query("SELECT * FROM pending_notifications ORDER BY id ASC LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<PendingNotification>

    @Query("DELETE FROM pending_notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_notifications SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("SELECT COUNT(*) FROM pending_notifications")
    fun pendingCount(): Flow<Int>

    @Query("DELETE FROM pending_notifications")
    suspend fun clear()
}
