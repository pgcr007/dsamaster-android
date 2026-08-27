package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DueReviewItem(
    val problemId: Long,
    val title: String,
    val difficulty: String,
    val timesReviewed: Int,
    val nextReviewDate: Long
)

/**
 * Surfaces problems that are due for spaced-repetition review, joining
 * UserProgress.nextReviewDate against Problem for display purposes.
 *
 * Note: "now" is captured once, when this ViewModel is created (i.e. when
 * the Dashboard is first composed this session). It won't reactively notice
 * a problem crossing its due date mid-session — reopening the app (or
 * navigating back to Dashboard after the process restarts) re-evaluates it.
 * That's fine for a single-user daily-use app.
 */
class ReviewQueueViewModel(
    private val userProgressRepository: UserProgressRepository,
    private val problemRepository: ProblemRepository
) : ViewModel() {

    val dueForReview: StateFlow<List<DueReviewItem>> = combine(
        userProgressRepository.getDueForReview(System.currentTimeMillis()),
        problemRepository.getAllProblems()
    ) { progressList, problems ->
        val problemsById = problems.associateBy { it.id }
        progressList.mapNotNull { progress ->
            val problem = problemsById[progress.problemId] ?: return@mapNotNull null
            DueReviewItem(
                problemId = progress.problemId,
                title = problem.title,
                difficulty = problem.difficulty,
                timesReviewed = progress.timesReviewed,
                nextReviewDate = progress.nextReviewDate ?: 0L
            )
        }.sortedBy { it.nextReviewDate }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}