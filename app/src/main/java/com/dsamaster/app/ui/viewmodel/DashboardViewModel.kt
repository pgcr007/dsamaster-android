package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.TopicRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import com.dsamaster.app.logic.StreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A single difficulty bucket's solved/total counts, for the progress-by-difficulty row. */
data class DifficultyProgress(
    val label: String,
    val solved: Int,
    val total: Int
)

/** The next problem the user should tackle — first not-yet-solved problem, in topic order. */
data class ContinueLearningItem(
    val problemId: Long,
    val title: String,
    val difficulty: String,
    val topicName: String
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    // Streak
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isStreakAtRisk: Boolean = false,
    val canUseFreeze: Boolean = true,
    val dailyGoal: Int = 1,
    val problemsSolvedToday: Int = 0,
    val recentEntries: List<StreakEntry> = emptyList(),
    // Overall progress
    val totalProblems: Int = 0,
    val solvedProblems: Int = 0,
    val difficultyBreakdown: List<DifficultyProgress> = emptyList(),
    // Suggestions
    val continueLearning: ContinueLearningItem? = null,
    val dueForReview: List<DueReviewItem> = emptyList()
) {
    val goalProgress: Float
        get() = if (dailyGoal <= 0) 1f else (problemsSolvedToday.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val overallProgressPercent: Int
        get() = if (totalProblems <= 0) 0 else ((solvedProblems * 100f) / totalProblems).toInt()
}

private val DIFFICULTY_ORDER = listOf("Easy", "Medium", "Hard")

class DashboardViewModel(
    private val streakRepository: StreakRepository,
    private val userPreferences: UserPreferences,
    private val problemRepository: ProblemRepository,
    private val userProgressRepository: UserProgressRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val calculator = StreakCalculator()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val uiState: StateFlow<DashboardUiState> = combine(
        streakRepository.getRecentStreakEntries(180),
        userPreferences.dailyGoal,
        problemRepository.getAllProblems(),
        userProgressRepository.getAllProgress(),
        topicRepository.getAllTopics()
    ) { entries, goal, problems, progress, topics ->
        val today = LocalDate.now()
        val todayKey = today.format(dateFormatter)
        val topicsById = topics.associateBy { it.id }
        val solvedIds = progress.filter { it.status == "solved" }.map { it.problemId }.toSet()

        val difficultyBreakdown = DIFFICULTY_ORDER.map { label ->
            val inBucket = problems.filter { it.difficulty == label }
            DifficultyProgress(
                label = label,
                solved = inBucket.count { it.id in solvedIds },
                total = inBucket.size
            )
        }

        val continueLearning = problems
            .sortedBy { it.id }
            .firstOrNull { it.id !in solvedIds }
            ?.let { problem ->
                ContinueLearningItem(
                    problemId = problem.id,
                    title = problem.title,
                    difficulty = problem.difficulty,
                    topicName = topicsById[problem.topicId]?.name ?: "General"
                )
            }

        val now = System.currentTimeMillis()
        val problemsById = problems.associateBy { it.id }
        val dueForReview = progress
            .filter { it.status == "solved" && it.nextReviewDate != null && it.nextReviewDate <= now }
            .mapNotNull { p ->
                val problem = problemsById[p.problemId] ?: return@mapNotNull null
                DueReviewItem(
                    problemId = p.problemId,
                    title = problem.title,
                    difficulty = problem.difficulty,
                    timesReviewed = p.timesReviewed,
                    nextReviewDate = p.nextReviewDate ?: 0L
                )
            }
            .sortedBy { it.nextReviewDate }

        DashboardUiState(
            isLoading = false,
            currentStreak = calculator.getCurrentStreak(entries, today),
            longestStreak = calculator.getLongestStreak(entries),
            isStreakAtRisk = calculator.isStreakAtRisk(entries, today),
            canUseFreeze = calculator.canUseFreeze(entries, today),
            dailyGoal = goal,
            problemsSolvedToday = entries.find { it.date == todayKey }?.problemsSolved ?: 0,
            recentEntries = entries,
            totalProblems = problems.size,
            solvedProblems = solvedIds.size,
            difficultyBreakdown = difficultyBreakdown,
            continueLearning = continueLearning,
            dueForReview = dueForReview
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    /** Debug-only helper (wired behind BuildConfig.DEBUG in the UI) to simulate solving a problem today. */
    fun recordProblemSolved() {
        viewModelScope.launch {
            val today = LocalDate.now().format(dateFormatter)
            val current = uiState.value.recentEntries.find { it.date == today }
            val updated = current?.copy(problemsSolved = current.problemsSolved + 1) ?: StreakEntry(
                date = today,
                minutesActive = 0,
                problemsSolved = 1,
                streakFreezeUsed = false
            )
            streakRepository.insertStreakEntry(updated)
        }
    }

    fun useStreakFreeze() {
        viewModelScope.launch {
            val today = LocalDate.now().format(dateFormatter)
            if (!calculator.canUseFreeze(uiState.value.recentEntries, LocalDate.now())) return@launch

            val current = uiState.value.recentEntries.find { it.date == today }
            val updated = current?.copy(streakFreezeUsed = true) ?: StreakEntry(
                date = today,
                minutesActive = 0,
                problemsSolved = 0,
                streakFreezeUsed = true
            )
            streakRepository.insertStreakEntry(updated)
        }
    }
}