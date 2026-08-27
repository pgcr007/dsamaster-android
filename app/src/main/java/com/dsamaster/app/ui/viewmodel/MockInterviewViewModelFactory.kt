package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class MockInterviewViewModelFactory(
    private val application: DsaMasterApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return MockInterviewViewModel(
            topicRepository = application.topicRepository,
            problemRepository = application.problemRepository,
            interviewRepository = application.interviewRepository,
            sessionRepository = application.mockInterviewSessionRepository
        ) as T
    }
}