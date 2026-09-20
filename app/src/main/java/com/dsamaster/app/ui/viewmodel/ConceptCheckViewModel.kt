package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.repository.ConceptCheckRepository
import com.dsamaster.app.data.repository.LearningProgressManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class ConceptCheckOption(val text: String, val index: Int)

data class ConceptCheckQuestion(
    val id: Long,
    val question: String,
    val options: List<ConceptCheckOption>,
    val correctOptionIndex: Int,
    val explanation: String
)

data class ConceptCheckUiState(
    val isLoading: Boolean = true,
    val questions: List<ConceptCheckQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val hasAnswered: Boolean = false,
    val correctCount: Int = 0,
    val isFinished: Boolean = false,
    val passed: Boolean = false
)

/**
 * Pass criterion: every question must be answered correctly. With only a
 * couple of concept checks per lesson, a partial-credit threshold would let
 * a real gap through — retrying costs nothing, so require all-correct.
 */
class ConceptCheckViewModel(
    private val conceptCheckRepository: ConceptCheckRepository,
    private val learningProgressManager: LearningProgressManager,
    private val lessonId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConceptCheckUiState())
    val uiState: StateFlow<ConceptCheckUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val checks = conceptCheckRepository.getChecksForLesson(lessonId).first()
            val questions = checks.map { check ->
                val options = Json.decodeFromString<List<String>>(check.optionsJson)
                ConceptCheckQuestion(
                    id = check.id,
                    question = check.question,
                    options = options.mapIndexed { index, text -> ConceptCheckOption(text, index) },
                    correctOptionIndex = check.correctOptionIndex,
                    explanation = check.explanation
                )
            }
            _uiState.value = ConceptCheckUiState(isLoading = false, questions = questions)
        }
    }

    fun onOptionSelected(optionIndex: Int) {
        val current = _uiState.value
        if (current.hasAnswered) return
        _uiState.value = current.copy(selectedOptionIndex = optionIndex, hasAnswered = true)
    }

    fun onNext() {
        val current = _uiState.value
        val question = current.questions.getOrNull(current.currentIndex) ?: return
        val wasCorrect = current.selectedOptionIndex == question.correctOptionIndex
        val newCorrectCount = current.correctCount + if (wasCorrect) 1 else 0
        val nextIndex = current.currentIndex + 1

        if (nextIndex >= current.questions.size) {
            val passed = newCorrectCount == current.questions.size
            _uiState.value = current.copy(
                correctCount = newCorrectCount,
                isFinished = true,
                passed = passed
            )
            if (passed) {
                viewModelScope.launch { learningProgressManager.completeLesson(lessonId) }
            }
        } else {
            _uiState.value = current.copy(
                currentIndex = nextIndex,
                selectedOptionIndex = null,
                hasAnswered = false,
                correctCount = newCorrectCount
            )
        }
    }

    fun onRetry() {
        _uiState.value = _uiState.value.copy(
            currentIndex = 0,
            selectedOptionIndex = null,
            hasAnswered = false,
            correctCount = 0,
            isFinished = false,
            passed = false
        )
    }
}