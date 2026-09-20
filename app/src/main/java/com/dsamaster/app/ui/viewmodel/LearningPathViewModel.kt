package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.dao.LessonOverviewRow
import com.dsamaster.app.data.repository.LearningPathRepository
import com.dsamaster.app.data.repository.LessonRepository
import com.dsamaster.app.data.seed.LearningContentSeeder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LessonListItem(
    val id: Long,
    val title: String,
    val status: String // "locked" | "unlocked" | "completed"
)

data class ModuleListItem(
    val id: Long,
    val title: String,
    val lessons: List<LessonListItem>
)

data class LearningPathUiState(
    val isLoading: Boolean = true,
    val pathName: String = "",
    val modules: List<ModuleListItem> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

class LearningPathViewModel(
    private val learningPathRepository: LearningPathRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    val uiState: StateFlow<LearningPathUiState> = learningPathRepository.getAllPaths()
        .map { paths -> paths.firstOrNull { it.name == LearningContentSeeder.FOUNDATIONS_PATH_NAME } }
        .flatMapLatest { path ->
            if (path == null) {
                flowOf(LearningPathUiState())
            } else {
                lessonRepository.getLearningPathOverview(path.id).map { rows ->
                    toUiState(path.name, rows)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LearningPathUiState()
        )

    private fun toUiState(pathName: String, rows: List<LessonOverviewRow>): LearningPathUiState {
        if (rows.isEmpty()) return LearningPathUiState(isLoading = true, pathName = pathName)

        val modules = rows
            .groupBy { it.moduleId }
            .entries
            .sortedBy { (_, moduleRows) -> moduleRows.first().moduleOrder }
            .map { (moduleId, moduleRows) ->
                val sortedLessons = moduleRows.sortedBy { it.lessonOrder }
                ModuleListItem(
                    id = moduleId,
                    title = sortedLessons.first().moduleTitle,
                    lessons = sortedLessons.map { row ->
                        LessonListItem(id = row.lessonId, title = row.lessonTitle, status = row.status)
                    }
                )
            }

        return LearningPathUiState(
            isLoading = false,
            pathName = pathName,
            modules = modules,
            completedCount = rows.count { it.status == "completed" },
            totalCount = rows.size
        )
    }
}