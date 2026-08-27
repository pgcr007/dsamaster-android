package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class ProblemViewModelFactory(
    private val application: DsaMasterApplication,
    private val initialTopicId: Long? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return ProblemViewModel(
            problemRepository = application.problemRepository,
            topicRepository = application.topicRepository,
            initialTopicId = initialTopicId
        ) as T
    }
}