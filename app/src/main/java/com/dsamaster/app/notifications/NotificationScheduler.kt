package com.dsamaster.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Schedules and cancels the Phase 10 background workers via WorkManager.
 *
 * WorkManager's PeriodicWorkRequest doesn't guarantee an exact fire time (the OS
 * batches work for battery reasons), but computing the initial delay to the next
 * occurrence of the target time keeps it accurate to within a small window, which
 * is the standard approach for "daily reminder at roughly HH:mm" notifications.
 */
object NotificationScheduler {

    // Fixed check points, independent of the user's configurable reminder time —
    // see Step 3 notes: streak-risk and weekly summary aren't user-configurable times.
    private val STREAK_RISK_TIME = LocalTime.of(21, 0) // 9:00 PM
    private val WEEKLY_SUMMARY_TIME = LocalTime.of(20, 0) // 8:00 PM
    private val WEEKLY_SUMMARY_DAY = DayOfWeek.SUNDAY

    /**
     * Reads current preferences and (re)schedules everything accordingly.
     * Safe to call repeatedly (e.g. app start, or after a Settings change) —
     * uses ExistingPeriodicWorkPolicy.UPDATE so it replaces rather than duplicates.
     */
    fun scheduleAll(
        context: Context,
        notificationsEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        weeklySummaryEnabled: Boolean
    ) {
        if (notificationsEnabled) {
            scheduleDailyReminder(context, reminderHour, reminderMinute)
            scheduleStreakRisk(context)
        } else {
            cancelDailyReminder(context)
            cancelStreakRisk(context)
        }

        if (notificationsEnabled && weeklySummaryEnabled) {
            scheduleWeeklySummary(context)
        } else {
            cancelWeeklySummary(context)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val initialDelay = delayUntilNextDailyOccurrence(LocalTime.of(hour, minute))

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .addTag(DailyReminderWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleStreakRisk(context: Context) {
        val initialDelay = delayUntilNextDailyOccurrence(STREAK_RISK_TIME)

        val request = PeriodicWorkRequestBuilder<StreakRiskWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .addTag(StreakRiskWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            StreakRiskWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleWeeklySummary(context: Context) {
        val initialDelay = delayUntilNextWeeklyOccurrence(WEEKLY_SUMMARY_DAY, WEEKLY_SUMMARY_TIME)

        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .addTag(WeeklySummaryWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeeklySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }

    fun cancelStreakRisk(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(StreakRiskWorker.WORK_NAME)
    }

    fun cancelWeeklySummary(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WeeklySummaryWorker.WORK_NAME)
    }

    // --- Debug/testing only: runs each worker once, immediately, bypassing the
    // normal daily/weekly schedule entirely. Not tied to real notification timing —
    // just lets you verify each worker's logic and notification content on demand.
    // Safe to leave in for now; remove or gate behind a BuildConfig.DEBUG check
    // before Phase 11 release polish if you don't want it in a release build.

    fun testRunDailyReminder(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun testRunStreakRisk(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<StreakRiskWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun testRunWeeklySummary(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<WeeklySummaryWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }


    private fun delayUntilNextDailyOccurrence(target: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(target)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next)
    }

    private fun delayUntilNextWeeklyOccurrence(day: DayOfWeek, target: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate()
            .with(TemporalAdjusters.nextOrSame(day))
            .atTime(target)
        if (!next.isAfter(now)) {
            next = next.plusWeeks(1)
        }
        return Duration.between(now, next)
    }
}