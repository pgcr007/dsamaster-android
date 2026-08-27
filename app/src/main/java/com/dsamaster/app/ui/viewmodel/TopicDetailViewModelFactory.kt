package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class TopicDetailViewModelFactory(
    private val application: DsaMasterApplication,
    private val topicId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return TopicDetailViewModel(
            topicRepository = application.topicRepository,
            topicId = topicId
        ) as T
    }
}