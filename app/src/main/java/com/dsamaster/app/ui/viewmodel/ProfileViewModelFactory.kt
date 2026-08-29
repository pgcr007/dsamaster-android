package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.remote.ProfileApiClient

class ProfileViewModelFactory(
    private val application: DsaMasterApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(
            userPreferences = application.userPreferences,
            userProgressRepository = application.userProgressRepository,
            problemRepository = application.problemRepository,
            streakRepository = application.streakRepository,
            profileApiClient = ProfileApiClient()
        ) as T
    }
}