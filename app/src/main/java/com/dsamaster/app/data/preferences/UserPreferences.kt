package com.dsamaster.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal")
        const val DEFAULT_DAILY_GOAL = 1

        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        const val DEFAULT_NOTIFICATIONS_ENABLED = true

        private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")
        const val DEFAULT_REMINDER_HOUR = 19 // 7:00 PM, 24hr format
        const val DEFAULT_REMINDER_MINUTE = 0

        private val WEEKLY_SUMMARY_ENABLED_KEY = booleanPreferencesKey("weekly_summary_enabled")
        const val DEFAULT_WEEKLY_SUMMARY_ENABLED = true

        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        const val THEME_MODE_SYSTEM = "SYSTEM"
        const val THEME_MODE_LIGHT = "LIGHT"
        const val THEME_MODE_DARK = "DARK"
        const val DEFAULT_THEME_MODE = THEME_MODE_SYSTEM

        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        const val DEFAULT_HAS_SEEN_ONBOARDING = false

        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")

        private val PROFILE_CACHE_KEY = stringPreferencesKey("profile_cache_json")
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DAILY_GOAL_KEY] ?: DEFAULT_DAILY_GOAL
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { prefs ->
            prefs[DAILY_GOAL_KEY] = goal
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED_KEY] ?: DEFAULT_NOTIFICATIONS_ENABLED
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[REMINDER_HOUR_KEY] ?: DEFAULT_REMINDER_HOUR
    }

    val reminderMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[REMINDER_MINUTE_KEY] ?: DEFAULT_REMINDER_MINUTE
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_HOUR_KEY] = hour
            prefs[REMINDER_MINUTE_KEY] = minute
        }
    }

    val weeklySummaryEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WEEKLY_SUMMARY_ENABLED_KEY] ?: DEFAULT_WEEKLY_SUMMARY_ENABLED
    }

    suspend fun setWeeklySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WEEKLY_SUMMARY_ENABLED_KEY] = enabled
        }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode
        }
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_SEEN_ONBOARDING_KEY] ?: DEFAULT_HAS_SEEN_ONBOARDING
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_SEEN_ONBOARDING_KEY] = seen
        }
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN_KEY]
    }

    val userEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY] ?: ""
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY] ?: ""
    }

    suspend fun setAuthSession(token: String, email: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN_KEY] = token
            prefs[USER_EMAIL_KEY] = email
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun clearAuthSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(PROFILE_CACHE_KEY)
        }
    }

    /** Cached JSON snapshot of the last-fetched ProfileDto, so ProfileScreen has
     *  something to show immediately while it refreshes from the backend. */
    val profileCacheJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PROFILE_CACHE_KEY] ?: ""
    }

    suspend fun setProfileCacheJson(json: String) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_CACHE_KEY] = json
        }
    }
}