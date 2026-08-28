package com.dsamaster.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.backup.BackupManager
import com.dsamaster.app.data.backup.BackupResult
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.AuthTokenStore
import com.dsamaster.app.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val dailyGoal: Int = 1,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val weeklySummaryEnabled: Boolean = true,
    val themeMode: String = UserPreferences.DEFAULT_THEME_MODE,
    val userEmail: String = "",
    val userName: String = ""
)

class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val appContext: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferences.dailyGoal,
        userPreferences.notificationsEnabled,
        userPreferences.reminderHour,
        userPreferences.reminderMinute,
        userPreferences.weeklySummaryEnabled,
        userPreferences.themeMode,
        userPreferences.userEmail,
        userPreferences.userName
    ) { values ->
        SettingsUiState(
            dailyGoal = values[0] as Int,
            notificationsEnabled = values[1] as Boolean,
            reminderHour = values[2] as Int,
            reminderMinute = values[3] as Int,
            weeklySummaryEnabled = values[4] as Boolean,
            themeMode = values[5] as String,
            userEmail = values[6] as String,
            userName = values[7] as String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            userPreferences.setDailyGoal(goal)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
            rescheduleNotifications()
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferences.setReminderTime(hour, minute)
            rescheduleNotifications()
        }
    }

    fun setWeeklySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setWeeklySummaryEnabled(enabled)
            rescheduleNotifications()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = when (val result = backupManager.exportTo(uri)) {
                is BackupResult.ExportSuccess -> "Backup saved — ${result.itemCount} items."
                is BackupResult.Failure -> result.message
                else -> null
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = when (val result = backupManager.restoreFrom(uri)) {
                is BackupResult.RestoreSuccess ->
                    "Restored ${result.itemCount} items. Restart the app to see everything reflected."
                is BackupResult.Failure -> result.message
                else -> null
            }
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun testRunDailyReminder() = NotificationScheduler.testRunDailyReminder(appContext)
    fun testRunStreakRisk() = NotificationScheduler.testRunStreakRisk(appContext)
    fun testRunWeeklySummary() = NotificationScheduler.testRunWeeklySummary(appContext)

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearAuthSession()
            AuthTokenStore.token = null
        }
    }

    private suspend fun rescheduleNotifications() {
        val state = uiState.value
        NotificationScheduler.scheduleAll(
            context = appContext,
            notificationsEnabled = state.notificationsEnabled,
            reminderHour = state.reminderHour,
            reminderMinute = state.reminderMinute,
            weeklySummaryEnabled = state.weeklySummaryEnabled
        )
    }
}