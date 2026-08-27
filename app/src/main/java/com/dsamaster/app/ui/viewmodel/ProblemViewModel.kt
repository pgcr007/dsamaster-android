package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProblemListItem(
    val problem: Problem,
    val topicName: String
)

data class ProblemFilter(
    val topicId: Long? = null,
    val difficulty: String? = null,
    val company: String? = null
)

class ProblemViewModel(
    private val problemRepository: ProblemRepository,
    private val topicRepository: TopicRepository,
    initialTopicId: Long? = null
) : ViewModel() {

    private val filterState = MutableStateFlow(ProblemFilter(topicId = initialTopicId))
    val filter: StateFlow<ProblemFilter> = filterState.asStateFlow()

    // Only topics that currently have at least one seeded problem — keeps the
    // filter row from listing dozens of topics with nothing to show yet.
    val topicsWithProblems: StateFlow<List<Topic>> = combine(
        problemRepository.getAllProblems(),
        topicRepository.getAllTopics()
    ) { problems, topics ->
        val topicIdsWithProblems = problems.map { it.topicId }.toSet()
        topics.filter { it.id in topicIdsWithProblems }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCompanies: StateFlow<List<String>> = problemRepository.getAllProblems()
        .map { problems ->
            problems.flatMap { it.companyTags.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProblems: StateFlow<List<ProblemListItem>> = combine(
        problemRepository.getAllProblems(),
        topicRepository.getAllTopics(),
        filterState
    ) { problems, topics, filter ->
        val topicNameById = topics.associateBy({ it.id }, { it.name })
        problems
            .filter { filter.topicId == null || it.topicId == filter.topicId }
            .filter { filter.difficulty == null || it.difficulty == filter.difficulty }
            .filter {
                filter.company == null ||
                        it.companyTags.split(",").map { c -> c.trim() }.contains(filter.company)
            }
            .map { ProblemListItem(it, topicNameById[it.topicId] ?: "Unknown") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTopicFilter(topicId: Long?) {
        filterState.value = filterState.value.copy(topicId = topicId)
    }

    fun setDifficultyFilter(difficulty: String?) {
        filterState.value = filterState.value.copy(difficulty = difficulty)
    }

    fun setCompanyFilter(company: String?) {
        filterState.value = filterState.value.copy(company = company)
    }
}