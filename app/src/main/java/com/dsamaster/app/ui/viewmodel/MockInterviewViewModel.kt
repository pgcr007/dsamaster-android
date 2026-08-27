package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.MockInterviewSession
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.remote.CodeTemplates
import com.dsamaster.app.data.remote.InterviewResult
import com.dsamaster.app.data.repository.InterviewRepository
import com.dsamaster.app.data.repository.MockInterviewSessionRepository
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class InterviewPhase { SETUP, APPROACH, CLARIFY, CODING, FOLLOWUP, SUMMARY }

data class MockInterviewUiState(
    val phase: InterviewPhase = InterviewPhase.SETUP,
    val topics: List<Topic> = emptyList(),
    val selectedTopicId: Long? = null, // null = any topic
    val selectedDifficulty: String? = null, // null = any difficulty
    val noMatchingProblems: Boolean = false,

    val problem: Problem? = null,

    val selectedLanguage: String = "python",
    val codeByLanguage: Map<String, String> = mapOf(
        "python" to CodeTemplates.PYTHON,
        "java" to CodeTemplates.JAVA,
        "cpp" to CodeTemplates.CPP
    ),

    val approachText: String = "",
    val isRequestingClarify: Boolean = false,
    val clarifyError: String? = null,

    val clarifyingQuestion: String = "",
    val clarifyingAcknowledgement: String = "",
    val clarifyingAnswer: String = "",

    val isRequestingFollowUp: Boolean = false,
    val followUpError: String? = null,
    val followUpQuestion: String = "",
    val followUpAnswer: String = "",

    val isRequestingSummary: Boolean = false,
    val summaryError: String? = null,
    val wentWell: List<String> = emptyList(),
    val workOn: List<String> = emptyList(),
    val overallNotes: String = "",

    val elapsedSeconds: Int = 0,

    val showHistory: Boolean = false,
    val pastSessions: List<MockInterviewSession> = emptyList()
) {
    val currentCode: String get() = codeByLanguage[selectedLanguage].orEmpty()
}

class MockInterviewViewModel(
    private val topicRepository: TopicRepository,
    private val problemRepository: ProblemRepository,
    private val interviewRepository: InterviewRepository,
    private val sessionRepository: MockInterviewSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockInterviewUiState())
    val uiState: StateFlow<MockInterviewUiState> = _uiState.asStateFlow()

    private var allProblems: List<Problem> = emptyList()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val topics = topicRepository.getAllTopics().first()
            _uiState.value = _uiState.value.copy(topics = topics)
        }
        problemRepository.getAllProblems()
            .onEach { allProblems = it }
            .launchIn(viewModelScope)
        sessionRepository.getAllSessions()
            .onEach { sessions -> _uiState.value = _uiState.value.copy(pastSessions = sessions) }
            .launchIn(viewModelScope)
    }

    fun onTopicSelected(topicId: Long?) {
        _uiState.value = _uiState.value.copy(selectedTopicId = topicId, noMatchingProblems = false)
    }

    fun onDifficultySelected(difficulty: String?) {
        _uiState.value = _uiState.value.copy(selectedDifficulty = difficulty, noMatchingProblems = false)
    }

    fun toggleHistory() {
        _uiState.value = _uiState.value.copy(showHistory = !_uiState.value.showHistory)
    }

    fun deleteSession(session: MockInterviewSession) {
        viewModelScope.launch { sessionRepository.deleteSession(session) }
    }

    fun startInterview() {
        val state = _uiState.value
        val candidates = allProblems.filter { p ->
            (state.selectedTopicId == null || p.topicId == state.selectedTopicId) &&
                    (state.selectedDifficulty == null || p.difficulty == state.selectedDifficulty)
        }

        if (candidates.isEmpty()) {
            _uiState.value = state.copy(noMatchingProblems = true)
            return
        }

        val chosen = candidates.random()
        _uiState.value = MockInterviewUiState(
            phase = InterviewPhase.APPROACH,
            topics = state.topics,
            selectedTopicId = state.selectedTopicId,
            selectedDifficulty = state.selectedDifficulty,
            problem = chosen,
            pastSessions = state.pastSessions
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.value = _uiState.value.copy(elapsedSeconds = _uiState.value.elapsedSeconds + 1)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun onApproachChanged(text: String) {
        _uiState.value = _uiState.value.copy(approachText = text)
    }

    fun submitApproach() {
        val state = _uiState.value
        val problem = state.problem ?: return
        if (state.isRequestingClarify || state.approachText.isBlank()) return

        _uiState.value = state.copy(isRequestingClarify = true, clarifyError = null)

        viewModelScope.launch {
            val result = interviewRepository.requestClarifyingQuestion(
                problemTitle = problem.title,
                problemDescription = problem.description,
                difficulty = problem.difficulty,
                approach = state.approachText
            )

            _uiState.value = when (result) {
                is InterviewResult.ClarifySuccess -> _uiState.value.copy(
                    isRequestingClarify = false,
                    phase = InterviewPhase.CLARIFY,
                    clarifyingQuestion = result.response.question,
                    clarifyingAcknowledgement = result.response.acknowledgement
                )
                is InterviewResult.Failure -> _uiState.value.copy(
                    isRequestingClarify = false,
                    clarifyError = result.message
                )
                else -> _uiState.value.copy(
                    isRequestingClarify = false,
                    clarifyError = "Unexpected response type from server."
                )
            }
        }
    }

    fun onClarifyingAnswerChanged(text: String) {
        _uiState.value = _uiState.value.copy(clarifyingAnswer = text)
    }

    fun proceedToCoding() {
        _uiState.value = _uiState.value.copy(phase = InterviewPhase.CODING)
    }

    fun onLanguageSelected(language: String) {
        if (language == _uiState.value.selectedLanguage) return
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun onCodeChanged(newCode: String) {
        val state = _uiState.value
        val updated = state.codeByLanguage.toMutableMap()
        updated[state.selectedLanguage] = newCode
        _uiState.value = state.copy(codeByLanguage = updated)
    }

    fun submitCode() {
        val state = _uiState.value
        val problem = state.problem ?: return
        if (state.isRequestingFollowUp || state.currentCode.isBlank()) return

        _uiState.value = state.copy(isRequestingFollowUp = true, followUpError = null)

        viewModelScope.launch {
            val result = interviewRepository.requestFollowUpQuestion(
                problemTitle = problem.title,
                problemDescription = problem.description,
                difficulty = problem.difficulty,
                approach = state.approachText,
                language = state.selectedLanguage,
                code = state.currentCode
            )

            _uiState.value = when (result) {
                is InterviewResult.FollowUpSuccess -> _uiState.value.copy(
                    isRequestingFollowUp = false,
                    phase = InterviewPhase.FOLLOWUP,
                    followUpQuestion = result.response.question
                )
                is InterviewResult.Failure -> _uiState.value.copy(
                    isRequestingFollowUp = false,
                    followUpError = result.message
                )
                else -> _uiState.value.copy(
                    isRequestingFollowUp = false,
                    followUpError = "Unexpected response type from server."
                )
            }
        }
    }

    fun onFollowUpAnswerChanged(text: String) {
        _uiState.value = _uiState.value.copy(followUpAnswer = text)
    }

    fun finishInterview() {
        val state = _uiState.value
        val problem = state.problem ?: return
        if (state.isRequestingSummary || state.followUpAnswer.isBlank()) return

        _uiState.value = state.copy(isRequestingSummary = true, summaryError = null)

        viewModelScope.launch {
            val result = interviewRepository.requestSessionSummary(
                problemTitle = problem.title,
                difficulty = problem.difficulty,
                approach = state.approachText,
                clarifyingQuestion = state.clarifyingQuestion,
                clarifyingAnswer = state.clarifyingAnswer,
                language = state.selectedLanguage,
                code = state.currentCode,
                followUpQuestion = state.followUpQuestion,
                followUpAnswer = state.followUpAnswer,
                durationSeconds = state.elapsedSeconds
            )

            when (result) {
                is InterviewResult.SummarySuccess -> {
                    stopTimer()
                    sessionRepository.insertSession(
                        MockInterviewSession(
                            problemId = problem.id,
                            problemTitle = problem.title,
                            difficulty = problem.difficulty,
                            language = state.selectedLanguage,
                            approach = state.approachText,
                            clarifyingQuestion = state.clarifyingQuestion,
                            clarifyingAnswer = state.clarifyingAnswer,
                            code = state.currentCode,
                            followUpQuestion = state.followUpQuestion,
                            followUpAnswer = state.followUpAnswer,
                            wentWellJson = MockInterviewSessionRepository.encodeStringList(result.response.wentWell),
                            workOnJson = MockInterviewSessionRepository.encodeStringList(result.response.workOn),
                            overallNotes = result.response.overallNotes,
                            durationSeconds = state.elapsedSeconds,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isRequestingSummary = false,
                        phase = InterviewPhase.SUMMARY,
                        wentWell = result.response.wentWell,
                        workOn = result.response.workOn,
                        overallNotes = result.response.overallNotes
                    )
                }
                is InterviewResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isRequestingSummary = false,
                        summaryError = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isRequestingSummary = false,
                        summaryError = "Unexpected response type from server."
                    )
                }
            }
        }
    }

    fun startNewInterview() {
        stopTimer()
        val state = _uiState.value
        _uiState.value = MockInterviewUiState(
            topics = state.topics,
            pastSessions = state.pastSessions
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}