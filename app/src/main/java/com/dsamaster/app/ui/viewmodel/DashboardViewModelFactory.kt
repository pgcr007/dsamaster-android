package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication

class DashboardViewModelFactory(
    private val application: DsaMasterApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(
            streakRepository = application.streakRepository,
            userPreferences = application.userPreferences,
            problemRepository = application.problemRepository,
            userProgressRepository = application.userProgressRepository,
            topicRepository = application.topicRepository
        ) as T
    }
}