package com.dsamaster.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dsamaster.app.DsaMasterApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Fires at the user's configured reminder time. Shows a nudge only if today's
 * daily goal hasn't already been met — no point nagging someone who already solved.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DsaMasterApplication

        val notificationsEnabled = app.userPreferences.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        val dailyGoal = app.userPreferences.dailyGoal.first()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayEntry = app.streakRepository.getStreakEntryByDate(today).first()

        val alreadyMetGoal = (todayEntry?.problemsSolved ?: 0) >= dailyGoal
        if (alreadyMetGoal) return Result.success()

        NotificationHelper.showDailyReminderNotification(applicationContext, dailyGoal)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reminder_worker"
    }
}