package com.dsamaster.app

import android.app.Application
import android.util.Log
import com.dsamaster.app.data.DSAMasterDatabase
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.ReviewRetryScheduler
import com.dsamaster.app.data.repository.CodeDraftRepository
import com.dsamaster.app.data.repository.CodeExecutionRepository
import com.dsamaster.app.data.repository.InterviewRepository
import com.dsamaster.app.data.repository.MockInterviewSessionRepository
import com.dsamaster.app.data.repository.NoteRepository
import com.dsamaster.app.data.repository.PendingReviewRequestRepository
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.ReviewRepository
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.TopicRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import com.dsamaster.app.data.seed.ProblemSeeder
import com.dsamaster.app.data.seed.TopicSeeder
import com.dsamaster.app.notifications.NotificationHelper
import com.dsamaster.app.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.dsamaster.app.data.backup.BackupManager

class DsaMasterApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    val database: DSAMasterDatabase by lazy { DSAMasterDatabase.getDatabase(this) }

    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    val topicRepository: TopicRepository by lazy { TopicRepository(database.topicDao()) }
    val problemRepository: ProblemRepository by lazy { ProblemRepository(database.problemDao()) }
    val userProgressRepository: UserProgressRepository by lazy { UserProgressRepository(database.userProgressDao()) }
    val streakRepository: StreakRepository by lazy { StreakRepository(database.streakDao()) }
    val noteRepository: NoteRepository by lazy { NoteRepository(database.noteDao()) }
    val codeExecutionRepository: CodeExecutionRepository by lazy { CodeExecutionRepository() }
    val codeDraftRepository: CodeDraftRepository by lazy { CodeDraftRepository(database.codeDraftDao()) }
    val reviewRepository: ReviewRepository by lazy { ReviewRepository() }
    val interviewRepository: InterviewRepository by lazy { InterviewRepository() }
    val mockInterviewSessionRepository: MockInterviewSessionRepository by lazy {
        MockInterviewSessionRepository(database.mockInterviewSessionDao())
    }
    val backupManager: com.dsamaster.app.data.backup.BackupManager by lazy {
        com.dsamaster.app.data.backup.BackupManager(
            context = this,
            userPreferences = userPreferences,
            userProgressRepository = userProgressRepository,
            streakRepository = streakRepository,
            noteRepository = noteRepository,
            codeDraftRepository = codeDraftRepository,
            mockInterviewSessionRepository = mockInterviewSessionRepository
        )
    }
    val pendingReviewRequestRepository: PendingReviewRequestRepository by lazy {
        PendingReviewRequestRepository(database.pendingReviewRequestDao())
    }

    override fun onCreate() {
        super.onCreate()
        val topicSeeder = TopicSeeder(this, topicRepository)
        val problemSeeder = ProblemSeeder(this, topicRepository, problemRepository)
        applicationScope.launch {
            try {
                topicSeeder.seedIfNeeded()
            } catch (e: Exception) {
                Log.e("DsaMasterApp", "Topic seeding failed", e)
            }
            try {
                problemSeeder.seedIfNeeded() // must run after topics, links by topic name
            } catch (e: Exception) {
                Log.e("DsaMasterApp", "Problem seeding failed", e)
            }
        }

        NotificationHelper.createNotificationChannels(this)

        applicationScope.launch {
            try {
                val notificationsEnabled = userPreferences.notificationsEnabled.first()
                val reminderHour = userPreferences.reminderHour.first()
                val reminderMinute = userPreferences.reminderMinute.first()
                val weeklySummaryEnabled = userPreferences.weeklySummaryEnabled.first()

                NotificationScheduler.scheduleAll(
                    context = this@DsaMasterApplication,
                    notificationsEnabled = notificationsEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    weeklySummaryEnabled = weeklySummaryEnabled
                )
            } catch (e: Exception) {
                Log.e("DsaMasterApp", "Notification scheduling failed", e)
            }
        }

        // Catch any requests still queued from a previous session (e.g. the app
        // was killed right after a failure, before the retry worker finished).
        applicationScope.launch {
            try {
                val leftover = pendingReviewRequestRepository.getAllOnce()
                if (leftover.isNotEmpty()) {
                    ReviewRetryScheduler.scheduleRetry(this@DsaMasterApplication)
                }
            } catch (e: Exception) {
                Log.e("DsaMasterApp", "Review retry re-scheduling failed", e)
            }
        }
    }
}