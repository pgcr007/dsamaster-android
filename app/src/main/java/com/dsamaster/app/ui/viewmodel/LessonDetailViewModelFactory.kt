package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class LessonDetailViewModelFactory(
    private val application: DsaMasterApplication,
    private val lessonId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return LessonDetailViewModel(
            lessonRepository = application.lessonRepository,
            conceptCheckRepository = application.conceptCheckRepository,
            learningProgressRepository = application.learningProgressRepository,
            learningProgressManager = application.learningProgressManager,
            lessonId = lessonId
        ) as T
    }
}