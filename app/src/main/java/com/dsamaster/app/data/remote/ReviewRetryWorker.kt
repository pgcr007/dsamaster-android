package com.dsamaster.app.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.notifications.NotificationHelper

/**
 * Drains the pending-review-request queue whenever WorkManager runs this
 * (constrained to require network — see ReviewRetryScheduler). Each item that
 * succeeds triggers a "your review is ready" notification since the original
 * screen may no longer be open. Items that keep failing are retried up to
 * MAX_RETRIES times before being dropped with a "gave up" notification.
 */
class ReviewRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DsaMasterApplication
        val pending = app.pendingReviewRequestRepository.getAllOnce()
        if (pending.isEmpty()) return Result.success()

        var anyStillPending = false

        for (item in pending) {
            val result = if (item.mode == "hint") {
                app.reviewRepository.requestHint(
                    code = item.code,
                    language = item.language,
                    problemTitle = item.problemTitle,
                    problemDescription = item.problemDescription,
                    difficulty = item.difficulty,
                    hintLevel = item.hintLevel ?: 1
                )
            } else {
                app.reviewRepository.requestReview(
                    code = item.code,
                    language = item.language,
                    problemTitle = item.problemTitle,
                    problemDescription = item.problemDescription,
                    difficulty = item.difficulty
                )
            }

            when (result) {
                is ReviewResult.ReviewSuccess, is ReviewResult.HintSuccess -> {
                    app.pendingReviewRequestRepository.remove(item)
                    NotificationHelper.showReviewReadyNotification(applicationContext, item.problemTitle)
                }
                is ReviewResult.Failure -> {
                    if (item.retryCount + 1 >= MAX_RETRIES) {
                        app.pendingReviewRequestRepository.remove(item)
                        NotificationHelper.showReviewFailedNotification(applicationContext, item.problemTitle)
                    } else {
                        app.pendingReviewRequestRepository.incrementRetryCount(item.id)
                        anyStillPending = true
                    }
                }
            }
        }

        // Result.retry() re-runs this same worker with WorkManager's exponential
        // backoff (set on the request in ReviewRetryScheduler) rather than us
        // managing delay timing by hand.
        return if (anyStillPending) Result.retry() else Result.success()
    }

    companion object {
        const val WORK_NAME = "review_retry_worker"
        private const val MAX_RETRIES = 5
    }
}