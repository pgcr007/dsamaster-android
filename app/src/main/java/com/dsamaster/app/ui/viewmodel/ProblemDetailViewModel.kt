package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProblemDetailUiState(
    val problem: Problem? = null,
    val topicName: String = ""
)

class ProblemDetailViewModel(
    problemRepository: ProblemRepository,
    topicRepository: TopicRepository,
    problemId: Long
) : ViewModel() {

    val uiState: StateFlow<ProblemDetailUiState> = combine(
        problemRepository.getProblemById(problemId),
        topicRepository.getAllTopics()
    ) { problem, topics ->
        val topicName = topics.firstOrNull { it.id == problem?.topicId }?.name ?: ""
        ProblemDetailUiState(problem = problem, topicName = topicName)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProblemDetailUiState()
    )
}