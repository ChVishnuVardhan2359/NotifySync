package com.notifysync.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notifysync.app.data.local.PendingNotification.Companion.STATUS_FAILED
import com.notifysync.app.data.local.PendingNotification.Companion.STATUS_PENDING
import com.notifysync.app.data.local.PendingNotification.Companion.STATUS_SYNCED
import com.notifysync.app.data.local.PendingNotification.Companion.STATUS_SYNCING
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** Insert; duplicates (same eventKey) are ignored, preventing double uploads. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: PendingNotification): Long

    /** Rows still needing upload (pending or previously failed, under the retry cap). */
    @Query(
        "SELECT * FROM pending_notifications " +
            "WHERE status IN ('$STATUS_PENDING','$STATUS_FAILED') AND attempts < :maxAttempts " +
            "ORDER BY id ASC LIMIT :limit",
    )
    suspend fun nextBatch(limit: Int, maxAttempts: Int): List<PendingNotification>

    @Query("UPDATE pending_notifications SET status = '$STATUS_SYNCING' WHERE id = :id")
    suspend fun markSyncing(id: Long)

    @Query("UPDATE pending_notifications SET status = '$STATUS_SYNCED' WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE pending_notifications SET status = '$STATUS_FAILED', attempts = attempts + 1 WHERE id = :id")
    suspend fun markFailed(id: Long)

    /** Recover rows left 'syncing' if the process was killed mid-upload. */
    @Query("UPDATE pending_notifications SET status = '$STATUS_PENDING' WHERE status = '$STATUS_SYNCING'")
    suspend fun resetStuck()

    /** Count of records still to be synced (for the UI badge). */
    @Query("SELECT COUNT(*) FROM pending_notifications WHERE status != '$STATUS_SYNCED'")
    fun pendingCount(): Flow<Int>

    /** Bound the table — drop old, already-synced rows. */
    @Query("DELETE FROM pending_notifications WHERE status = '$STATUS_SYNCED' AND createdAt < :before")
    suspend fun pruneSynced(before: Long)

    @Query("DELETE FROM pending_notifications")
    suspend fun clear()
}
