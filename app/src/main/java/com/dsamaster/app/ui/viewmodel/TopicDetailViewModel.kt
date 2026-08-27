package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TopicDetailViewModel(
    topicRepository: TopicRepository,
    topicId: Long
) : ViewModel() {

    val topic: StateFlow<Topic?> = topicRepository.getTopicById(topicId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}