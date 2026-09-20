package com.dsamaster.app

import android.app.Application
import android.util.Log
import com.dsamaster.app.data.DSAMasterDatabase
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.ReviewRetryScheduler
import com.dsamaster.app.data.remote.SyncApiClient
import com.dsamaster.app.data.repository.CodeDraftRepository
import com.dsamaster.app.data.repository.CodeExecutionRepository
import com.dsamaster.app.data.repository.ConceptCheckRepository
import com.dsamaster.app.data.repository.InterviewRepository
import com.dsamaster.app.data.repository.LearningModuleRepository
import com.dsamaster.app.data.repository.LearningPathRepository
import com.dsamaster.app.data.repository.LearningProgressRepository
import com.dsamaster.app.data.repository.LessonRepository
import com.dsamaster.app.data.repository.MockInterviewSessionRepository
import com.dsamaster.app.data.repository.NoteRepository
import com.dsamaster.app.data.repository.PendingReviewRequestRepository
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.ReviewRepository
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.TopicRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import com.dsamaster.app.data.seed.LearningContentSeeder
import com.dsamaster.app.data.seed.ProblemSeeder
import com.dsamaster.app.data.seed.TopicSeeder
import com.dsamaster.app.data.sync.SyncManager
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

    // Phase 14 — Learning Module
    val learningPathRepository: LearningPathRepository by lazy {
        LearningPathRepository(database.learningPathDao())
    }
    val learningModuleRepository: LearningModuleRepository by lazy {
        LearningModuleRepository(database.learningModuleDao())
    }
    val lessonRepository: LessonRepository by lazy {
        LessonRepository(database.lessonDao())
    }
    val conceptCheckRepository: ConceptCheckRepository by lazy {
        ConceptCheckRepository(database.conceptCheckDao())
    }
    val learningProgressRepository: LearningProgressRepository by lazy {
        LearningProgressRepository(database.learningProgressDao())
    }

    val learningProgressManager: com.dsamaster.app.data.repository.LearningProgressManager by lazy {
        com.dsamaster.app.data.repository.LearningProgressManager(
            lessonRepository = lessonRepository,
            learningModuleRepository = learningModuleRepository,
            learningProgressRepository = learningProgressRepository
        )
    }

    /**
     * Binds userProgressRepository / streakRepository to whichever account
     * (Google or email/password) is currently signed in. Call
     * `syncManager.syncAfterLogin(userId)` right after a successful
     * login/register.
     */
    val syncManager: SyncManager by lazy {
        SyncManager(
            userProgressRepository = userProgressRepository,
            streakRepository = streakRepository,
            userPreferences = userPreferences,
            syncApiClient = SyncApiClient()
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Every local progress/streak write is pushed to the signed-in
        // account's cloud copy in the background. Wired here (rather than in
        // the repository constructors) to avoid a circular dependency
        // between the repositories and SyncManager.
        userProgressRepository.onWrite = { progress -> syncManager.pushProgressAsync(progress) }
        streakRepository.onWrite = { entry -> syncManager.pushStreakAsync(entry) }

        val topicSeeder = TopicSeeder(this, topicRepository)
        val problemSeeder = ProblemSeeder(this, topicRepository, problemRepository)
        val learningContentSeeder = LearningContentSeeder(
            context = this,
            learningPathRepository = learningPathRepository,
            learningModuleRepository = learningModuleRepository,
            lessonRepository = lessonRepository,
            conceptCheckRepository = conceptCheckRepository,
            learningProgressRepository = learningProgressRepository
        )
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
            try {
                learningContentSeeder.seedIfNeeded()
            } catch (e: Exception) {
                Log.e("DsaMasterApp", "Learning content seeding failed", e)
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