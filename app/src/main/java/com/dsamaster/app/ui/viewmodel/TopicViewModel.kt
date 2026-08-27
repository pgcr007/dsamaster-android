package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TopicViewModel(
    private val topicRepository: TopicRepository
) : ViewModel() {

    val topicsByCategory: StateFlow<Map<String, List<Topic>>> = topicRepository.getAllTopics()
        .map { topics -> topics.groupBy { it.category } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
}