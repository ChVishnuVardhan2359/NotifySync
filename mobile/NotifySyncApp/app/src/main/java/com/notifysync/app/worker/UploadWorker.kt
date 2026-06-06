package com.notifysync.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.DeviceDataRepository
import com.notifysync.app.data.repository.SyncRepository
import com.notifysync.app.service.NotifyListenerService
import com.notifysync.app.util.BackgroundLog
import java.util.concurrent.TimeUnit

/**
 * Drains the offline queue (heartbeat + capture current notifications + flush + calls/SMS).
 * Retried with exponential backoff by WorkManager on failure. Runs as a one-time job (triggered
 * by capture / connectivity) and as a periodic recovery job (~every 15 min, WorkManager minimum).
 */
class UploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tokenStore = TokenStore(applicationContext)
        if (tokenStore.token().isNullOrBlank() || !tokenStore.syncEnabled()) return Result.success()

        val repo = SyncRepository(applicationContext, tokenStore)
        val data = DeviceDataRepository(applicationContext, tokenStore)
        BackgroundLog.sync("worker run")

        runCatching {
            repo.heartbeat()
            NotifyListenerService.instance?.captureActiveNotifications()
        }
        val drained = repo.uploadPending()
        runCatching { data.syncCalls(); data.syncSms() }

        return if (drained) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "notifysync-upload"
        private const val PERIODIC_NAME = "notifysync-periodic"

        private fun constraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** One-shot flush (real-time triggers: new capture, connectivity, boot). */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        /** Periodic recovery sync — survives process death and reboots (WorkManager persists it). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
