package com.dsamaster.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dsamaster.app.MainActivity
import com.dsamaster.app.R

/**
 * Central place for notification channel setup and notification building/showing.
 * No Android permission checks are skipped here — every show*() call verifies
 * POST_NOTIFICATIONS is granted (required at runtime on API 33+) before posting,
 * and the notify() call itself is additionally wrapped in try/catch for
 * SecurityException as a defensive measure against permission being revoked in
 * the gap between the check and the call (also satisfies Android Studio's lint,
 * which can't trace permission checks through a custom helper function).
 */
object NotificationHelper {

    const val CHANNEL_ID_REMINDERS = "dsamaster_reminders"
    const val CHANNEL_ID_SUMMARY = "dsamaster_summary"
    const val CHANNEL_ID_AI_REVIEW = "dsamaster_ai_review"

    private const val NOTIFICATION_ID_DAILY_REMINDER = 1001
    private const val NOTIFICATION_ID_STREAK_RISK = 1002
    private const val NOTIFICATION_ID_WEEKLY_SUMMARY = 1003
    private const val NOTIFICATION_ID_REVIEW_READY = 1004
    private const val NOTIFICATION_ID_REVIEW_FAILED = 1005

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val remindersChannel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            "Daily reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily practice reminders and streak-at-risk nudges"
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_ID_SUMMARY,
            "Weekly summary",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Weekly recap of problems solved and topics reviewed"
        }

        val aiReviewChannel = NotificationChannel(
            CHANNEL_ID_AI_REVIEW,
            "AI review results",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Queued AI code reviews and hints that finished in the background"
        }

        manager.createNotificationChannel(remindersChannel)
        manager.createNotificationChannel(summaryChannel)
        manager.createNotificationChannel(aiReviewChannel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun contentPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun safeNotify(context: Context, notificationId: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the hasNotificationPermission() check and
            // this call — nothing to do but skip showing the notification.
        }
    }

    fun showDailyReminderNotification(context: Context, dailyGoal: Int) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for your daily DSA practice")
            .setContentText(
                if (dailyGoal <= 1) "Solve today's problem to keep your streak alive."
                else "Solve $dailyGoal problems today to hit your goal."
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .build()

        safeNotify(context, NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    fun showStreakRiskNotification(context: Context, currentStreak: Int) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your streak is about to break")
            .setContentText(
                if (currentStreak > 0) "Your $currentStreak-day streak ends at midnight — solve one problem to save it."
                else "Solve one problem today to start a new streak."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .build()

        safeNotify(context, NOTIFICATION_ID_STREAK_RISK, notification)
    }

    fun showWeeklySummaryNotification(
        context: Context,
        problemsSolved: Int,
        topicsReviewed: Int
    ) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your week in DSA")
            .setContentText("$problemsSolved problems solved, $topicsReviewed reviews done this week.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$problemsSolved problems solved and $topicsReviewed reviews completed this week. Keep it up!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .build()

        safeNotify(context, NOTIFICATION_ID_WEEKLY_SUMMARY, notification)
    }

    fun showReviewReadyNotification(context: Context, problemTitle: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_AI_REVIEW)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your queued AI review is ready")
            .setContentText("$problemTitle — open the app to see it.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .build()

        safeNotify(context, NOTIFICATION_ID_REVIEW_READY, notification)
    }

    fun showReviewFailedNotification(context: Context, problemTitle: String) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_AI_REVIEW)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Couldn't complete a queued AI review")
            .setContentText("$problemTitle — gave up after several attempts. Try again from the app.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .build()

        safeNotify(context, NOTIFICATION_ID_REVIEW_FAILED, notification)
    }
}