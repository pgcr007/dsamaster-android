package com.dsamaster.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.PendingReviewRequest
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.entity.UserProgress
import com.dsamaster.app.data.remote.CodeTemplates
import com.dsamaster.app.data.remote.ExecuteResult
import com.dsamaster.app.data.remote.ReviewResult
import com.dsamaster.app.data.remote.ReviewRetryScheduler
import com.dsamaster.app.data.remote.dto.ReviewResponse
import com.dsamaster.app.data.repository.CodeDraftRepository
import com.dsamaster.app.data.repository.CodeExecutionRepository
import com.dsamaster.app.data.repository.PendingReviewRequestRepository
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.ReviewRepository
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import com.dsamaster.app.data.seed.TestCaseSeed
import com.dsamaster.app.logic.SpacedRepetitionScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate

data class CodeEditorUiState(
    val problem: Problem? = null,
    val selectedLanguage: String = "python",
    val codeByLanguage: Map<String, String> = mapOf(
        "python" to CodeTemplates.PYTHON,
        "java" to CodeTemplates.JAVA,
        "cpp" to CodeTemplates.CPP
    ),
    val isRunning: Boolean = false,
    val executeResult: ExecuteResult? = null,
    val justMarkedSolved: Boolean = false,
    val isReviewing: Boolean = false,
    val reviewResult: ReviewResponse? = null,
    val reviewError: String? = null,
    val isRequestingHint: Boolean = false,
    val hintLevel: Int = 0,
    val hintText: String? = null,
    val hintError: String? = null,
    val isReviewMode: Boolean = false
) {
    val currentCode: String get() = codeByLanguage[selectedLanguage].orEmpty()
}

class CodeEditorViewModel(
    private val problemRepository: ProblemRepository,
    private val codeExecutionRepository: CodeExecutionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val streakRepository: StreakRepository,
    private val codeDraftRepository: CodeDraftRepository,
    private val reviewRepository: ReviewRepository,
    private val pendingReviewRequestRepository: PendingReviewRequestRepository,
    private val appContext: Context,
    private val problemId: Long,
    private val isReviewMode: Boolean = false
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(CodeEditorUiState(isReviewMode = isReviewMode))
    val uiState: StateFlow<CodeEditorUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val problem = problemRepository.getProblemById(problemId).first()

            // Review mode starts cold: solve it blank, don't pre-fill from a saved
            // draft. This forces genuine recall instead of just re-pasting old code.
            if (isReviewMode) {
                _uiState.value = _uiState.value.copy(problem = problem)
            } else {
                val drafts = codeDraftRepository.getDraftsForProblem(problemId)
                val codeMap = _uiState.value.codeByLanguage.toMutableMap()
                drafts.forEach { draft -> codeMap[draft.language] = draft.code }
                _uiState.value = _uiState.value.copy(problem = problem, codeByLanguage = codeMap)
            }
        }
    }

    fun onLanguageSelected(language: String) {
        val state = _uiState.value
        if (language == state.selectedLanguage) return

        // Flush any pending debounced save immediately before switching away,
        // so a quick language swap right after typing never loses work.
        // Skipped in review mode — a cold-review attempt should never
        // overwrite the real saved draft.
        if (!isReviewMode) {
            saveJob?.cancel()
            val languageBeingLeft = state.selectedLanguage
            val codeBeingLeft = state.currentCode
            viewModelScope.launch {
                codeDraftRepository.saveDraft(problemId, languageBeingLeft, codeBeingLeft)
            }
        }

        _uiState.value = state.copy(selectedLanguage = language, executeResult = null)
    }

    fun onCodeChanged(newCode: String) {
        val state = _uiState.value
        val updated = state.codeByLanguage.toMutableMap()
        updated[state.selectedLanguage] = newCode
        _uiState.value = state.copy(codeByLanguage = updated)

        if (!isReviewMode) {
            scheduleSave(state.selectedLanguage, newCode)
        }
    }

    private fun scheduleSave(language: String, code: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            codeDraftRepository.saveDraft(problemId, language, code)
        }
    }

    fun runCode() {
        val state = _uiState.value
        val problem = state.problem ?: return
        if (state.isRunning) return

        val testCases = runCatching {
            json.decodeFromString<List<TestCaseSeed>>(problem.testCasesJson)
        }.getOrDefault(emptyList())

        if (testCases.isEmpty()) {
            _uiState.value = state.copy(
                executeResult = ExecuteResult.Failure("This problem has no test cases to run against.")
            )
            return
        }

        _uiState.value = state.copy(isRunning = true, executeResult = null, justMarkedSolved = false)

        viewModelScope.launch {
            val result = codeExecutionRepository.runCode(
                sourceCode = state.currentCode,
                language = state.selectedLanguage,
                testCases = testCases.map { it.input to it.output }
            )

            val allPassed = (result as? ExecuteResult.Success)?.response?.allPassed == true
            if (allPassed) {
                markSolved()
            }

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                executeResult = result,
                justMarkedSolved = allPassed
            )
        }
    }

    private suspend fun markSolved() {
        val now = System.currentTimeMillis()
        val existingProgress = userProgressRepository.getProgressForProblem(problemId).first()

        // Only advance the spaced-repetition schedule on a genuinely new solve —
        // the first time ever, or once the problem was actually due for review.
        // Re-running already-passing code in the same sitting (e.g. re-hitting
        // Run after a language switch) shouldn't fast-forward the schedule.
        val isGenuineNewSolve = existingProgress == null ||
                existingProgress.status != "solved" ||
                (existingProgress.nextReviewDate != null && now >= existingProgress.nextReviewDate)

        val newTimesReviewed = if (isGenuineNewSolve) {
            (existingProgress?.timesReviewed ?: 0) + 1
        } else {
            existingProgress?.timesReviewed ?: 0
        }

        val nextReviewDate = if (isGenuineNewSolve) {
            SpacedRepetitionScheduler.computeNextReviewDate(newTimesReviewed, now)
        } else {
            existingProgress?.nextReviewDate
        }

        userProgressRepository.insertProgress(
            UserProgress(
                id = existingProgress?.id ?: 0,
                problemId = problemId,
                status = "solved",
                lastAttemptDate = now,
                timesReviewed = newTimesReviewed,
                nextReviewDate = nextReviewDate
            )
        )

        val today = LocalDate.now().toString()
        val existingEntry = streakRepository.getStreakEntryByDate(today).first()
        streakRepository.insertStreakEntry(
            StreakEntry(
                id = existingEntry?.id ?: 0,
                date = today,
                minutesActive = existingEntry?.minutesActive ?: 0,
                problemsSolved = (existingEntry?.problemsSolved ?: 0) + 1,
                streakFreezeUsed = existingEntry?.streakFreezeUsed ?: false
            )
        )
    }

    fun requestReview() {
        val state = _uiState.value
        val problem = state.problem ?: return
        if (state.isReviewing) return

        _uiState.value = state.copy(isReviewing = true, reviewResult = null, reviewError = null)

        viewModelScope.launch {
            val result = reviewRepository.requestReview(
                code = state.currentCode,
                language = state.selectedLanguage,
                problemTitle = problem.title,
                problemDescription = problem.description,
                difficulty = problem.difficulty
            )

            _uiState.value = when (result) {
                is ReviewResult.ReviewSuccess -> _uiState.value.copy(
                    isReviewing = false,
                    reviewResult = result.response
                )
                is ReviewResult.HintSuccess -> _uiState.value.copy(
                    isReviewing = false,
                    reviewError = "Unexpected response type from server."
                )
                is ReviewResult.Failure -> {
                    if (result.retryable) {
                        queueForRetry(problem, mode = "review", hintLevel = null)
                    }
                    _uiState.value.copy(
                        isReviewing = false,
                        reviewError = if (result.retryable) {
                            "${result.message} Queued — it'll retry automatically once you're back online."
                        } else {
                            result.message
                        }
                    )
                }
            }
        }
    }

    fun requestHint() {
        val state = _uiState.value
        val problem = state.problem ?: return
        if (state.isRequestingHint) return

        // Each tap escalates: no hint yet -> level 1, otherwise +1 capped at 3.
        val nextLevel = if (state.hintLevel == 0) 1 else minOf(state.hintLevel + 1, 3)

        _uiState.value = state.copy(isRequestingHint = true, hintError = null)

        viewModelScope.launch {
            val result = reviewRepository.requestHint(
                code = state.currentCode,
                language = state.selectedLanguage,
                problemTitle = problem.title,
                problemDescription = problem.description,
                difficulty = problem.difficulty,
                hintLevel = nextLevel
            )

            _uiState.value = when (result) {
                is ReviewResult.HintSuccess -> _uiState.value.copy(
                    isRequestingHint = false,
                    hintLevel = result.response.hintLevel,
                    hintText = result.response.hint
                )
                is ReviewResult.ReviewSuccess -> _uiState.value.copy(
                    isRequestingHint = false,
                    hintError = "Unexpected response type from server."
                )
                is ReviewResult.Failure -> {
                    if (result.retryable) {
                        queueForRetry(problem, mode = "hint", hintLevel = nextLevel)
                    }
                    _uiState.value.copy(
                        isRequestingHint = false,
                        hintError = if (result.retryable) {
                            "${result.message} Queued — it'll retry automatically once you're back online."
                        } else {
                            result.message
                        }
                    )
                }
            }
        }
    }

    private fun queueForRetry(problem: Problem, mode: String, hintLevel: Int?) {
        val state = _uiState.value
        viewModelScope.launch {
            pendingReviewRequestRepository.enqueue(
                PendingReviewRequest(
                    problemId = problemId,
                    problemTitle = problem.title,
                    problemDescription = problem.description,
                    difficulty = problem.difficulty,
                    code = state.currentCode,
                    language = state.selectedLanguage,
                    mode = mode,
                    hintLevel = hintLevel,
                    createdAt = System.currentTimeMillis()
                )
            )
            ReviewRetryScheduler.scheduleRetry(appContext)
        }
    }
}