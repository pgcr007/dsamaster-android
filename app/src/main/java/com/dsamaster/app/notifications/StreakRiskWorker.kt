package com.dsamaster.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.logic.StreakCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Runs at a fixed evening check point (9 PM, scheduled in NotificationScheduler).
 * Fires only if StreakCalculator says the streak is genuinely at risk — i.e.
 * yesterday counted but today doesn't yet.
 */
class StreakRiskWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val calculator = StreakCalculator()

    override suspend fun doWork(): Result {
        val app = applicationContext as DsaMasterApplication

        val notificationsEnabled = app.userPreferences.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        val entries = app.streakRepository.getRecentStreakEntries(30).first()
        val today = LocalDate.now()

        if (!calculator.isStreakAtRisk(entries, today)) return Result.success()

        // Streak *as of yesterday* — today hasn't counted yet, so report the streak
        // the user stands to lose, not the (already-broken-looking) streak as of today.
        val currentStreak = calculator.getCurrentStreak(entries, today.minusDays(1))

        NotificationHelper.showStreakRiskNotification(applicationContext, currentStreak)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "streak_risk_worker"
    }
}