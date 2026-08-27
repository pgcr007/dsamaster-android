package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class CodeEditorViewModelFactory(
    private val application: DsaMasterApplication,
    private val problemId: Long,
    private val isReviewMode: Boolean = false
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return CodeEditorViewModel(
            problemRepository = application.problemRepository,
            codeExecutionRepository = application.codeExecutionRepository,
            userProgressRepository = application.userProgressRepository,
            streakRepository = application.streakRepository,
            codeDraftRepository = application.codeDraftRepository,
            reviewRepository = application.reviewRepository,
            pendingReviewRequestRepository = application.pendingReviewRequestRepository,
            appContext = application.applicationContext,
            problemId = problemId,
            isReviewMode = isReviewMode
        ) as T
    }
}