package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.repository.ConceptCheckRepository
import com.dsamaster.app.data.repository.LearningProgressManager
import com.dsamaster.app.data.repository.LearningProgressRepository
import com.dsamaster.app.data.repository.LessonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val content: String = "",
    val diagramType: String? = null,
    val codeExample: String? = null,
    val status: String = "locked",
    val hasConceptChecks: Boolean = false
)

class LessonDetailViewModel(
    private val lessonRepository: LessonRepository,
    conceptCheckRepository: ConceptCheckRepository,
    private val learningProgressRepository: LearningProgressRepository,
    private val learningProgressManager: LearningProgressManager,
    private val lessonId: Long
) : ViewModel() {

    val uiState: StateFlow<LessonDetailUiState> = combine(
        lessonRepository.getLessonById(lessonId),
        conceptCheckRepository.getChecksForLesson(lessonId),
        learningProgressRepository.getProgressForLesson(lessonId)
    ) { lesson, checks, progress ->
        LessonDetailUiState(
            isLoading = lesson == null,
            title = lesson?.title.orEmpty(),
            content = lesson?.content.orEmpty(),
            diagramType = lesson?.diagramType,
            codeExample = lesson?.codeExample,
            status = progress?.status ?: "locked",
            hasConceptChecks = checks.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LessonDetailUiState()
    )

    /** Used only for lessons with no concept checks at all. */
    fun markCompleteWithoutConceptCheck() {
        viewModelScope.launch {
            learningProgressManager.completeLesson(lessonId)
        }
    }
}