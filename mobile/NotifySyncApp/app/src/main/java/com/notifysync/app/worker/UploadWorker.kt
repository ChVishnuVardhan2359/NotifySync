package com.notifysync.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.data.repository.SyncRepository
import java.util.concurrent.TimeUnit

/** Drains the offline notification queue; retried with backoff by WorkManager on failure. */
class UploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tokenStore = TokenStore(applicationContext)
        if (!tokenStore.syncEnabled()) return Result.success()

        val repo = SyncRepository(applicationContext, tokenStore)
        repo.heartbeat()
        val drained = repo.uploadPending()
        return if (drained) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "notifysync-upload"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
