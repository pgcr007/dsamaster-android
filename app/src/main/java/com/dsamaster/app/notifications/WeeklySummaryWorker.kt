package com.dsamaster.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dsamaster.app.DsaMasterApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * Runs weekly (Sunday evening, scheduled in NotificationScheduler). Summarizes
 * problems solved from StreakEntry rows and reviews completed from UserProgress
 * (timesReviewed >= 1 AND lastAttemptDate within the last 7 days — an approximation,
 * since the schema doesn't separately timestamp each individual review event).
 */
class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DsaMasterApplication

        val weeklySummaryEnabled = app.userPreferences.weeklySummaryEnabled.first()
        if (!weeklySummaryEnabled) return Result.success()

        val recentEntries = app.streakRepository.getRecentStreakEntries(7).first()
        val problemsSolved = recentEntries.sumOf { it.problemsSolved }

        val sevenDaysAgoMillis = LocalDate.now()
            .minusDays(7)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val allProgress = app.userProgressRepository.getAllProgress().first()
        val reviewsCompleted = allProgress.count { progress ->
            progress.timesReviewed >= 1 &&
                    (progress.lastAttemptDate ?: 0L) >= sevenDaysAgoMillis
        }

        NotificationHelper.showWeeklySummaryNotification(
            applicationContext,
            problemsSolved,
            reviewsCompleted
        )
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "weekly_summary_worker"
    }
}