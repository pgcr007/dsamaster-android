package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class ProblemDetailViewModelFactory(
    private val application: DsaMasterApplication,
    private val problemId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return ProblemDetailViewModel(
            problemRepository = application.problemRepository,
            topicRepository = application.topicRepository,
            problemId = problemId
        ) as T
    }
}