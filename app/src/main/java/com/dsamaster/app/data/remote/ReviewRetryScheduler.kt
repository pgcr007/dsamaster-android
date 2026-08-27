package com.dsamaster.app.data.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

object ReviewRetryScheduler {

    /**
     * Enqueues (or leaves alone, if already waiting) a network-constrained retry.
     * KEEP rather than REPLACE: if a retry is already scheduled and waiting for
     * connectivity, queueing a second failed request shouldn't reset its backoff —
     * the worker re-reads all pending items fresh from Room on every run anyway.
     */
    fun scheduleRetry(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ReviewRetryWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(ReviewRetryWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ReviewRetryWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}