package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.TopicRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A topic ready for display in the list — pre-computed summary + problem progress. */
data class TopicListItem(
    val id: Long,
    val name: String,
    val category: String,
    val summary: String,
    val timeComplexity: String,
    val difficultyLevel: String,
    val companyTags: List<String>,
    val solvedProblems: Int,
    val totalProblems: Int
)

data class TopicsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val totalTopics: Int = 0,
    val groupedTopics: Map<String, List<TopicListItem>> = emptyMap()
)

class TopicViewModel(
    private val topicRepository: TopicRepository,
    private val problemRepository: ProblemRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TopicsUiState> = combine(
        topicRepository.getAllTopics(),
        problemRepository.getAllProblems(),
        userProgressRepository.getAllProgress(),
        searchQuery,
        selectedCategory
    ) { topics, problems, progress, query, category ->
        val categories = topics.map { it.category }.distinct()
        val solvedIds = progress.filter { it.status == "solved" }.map { it.problemId }.toSet()
        val problemsByTopic = problems.groupBy { it.topicId }

        val items = topics.map { topic ->
            val topicProblems = problemsByTopic[topic.id].orEmpty()
            TopicListItem(
                id = topic.id,
                name = topic.name,
                category = topic.category,
                summary = firstSentence(topic.explanation),
                timeComplexity = topic.timeComplexity,
                difficultyLevel = topic.difficultyLevel,
                companyTags = topic.companyTags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                solvedProblems = topicProblems.count { it.id in solvedIds },
                totalProblems = topicProblems.size
            )
        }

        val filtered = items
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .filter { query.isNotBlank() || category == null || it.category == category }

        // Preserve category encounter order from the original topic list.
        val grouped = LinkedHashMap<String, List<TopicListItem>>()
        categories.forEach { cat ->
            val topicsInCategory = filtered.filter { it.category == cat }
            if (topicsInCategory.isNotEmpty()) {
                grouped[cat] = topicsInCategory
            }
        }

        TopicsUiState(
            isLoading = false,
            searchQuery = query,
            selectedCategory = category,
            categories = categories,
            totalTopics = topics.size,
            groupedTopics = grouped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TopicsUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.value = category
    }

    private fun firstSentence(text: String): String {
        val trimmed = text.trim()
        val periodIndex = trimmed.indexOf(". ")
        return if (periodIndex in 1..119) {
            trimmed.substring(0, periodIndex + 1)
        } else if (trimmed.length > 120) {
            trimmed.take(117).trimEnd() + "…"
        } else {
            trimmed
        }
    }
}