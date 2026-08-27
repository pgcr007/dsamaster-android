package com.dsamaster.app.data.backup

import com.dsamaster.app.data.entity.CodeDraft
import com.dsamaster.app.data.entity.MockInterviewSession
import com.dsamaster.app.data.entity.Note
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.entity.UserProgress
import kotlinx.serialization.Serializable

/**
 * Everything a restore needs. Topics and Problems are deliberately excluded —
 * they're re-seeded from bundled assets on every install, so backing them up
 * would just bloat the file with content the app already has.
 */
@Serializable
data class BackupBundle(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val preferences: BackupPreferences,
    val userProgress: List<UserProgress>,
    val streakEntries: List<StreakEntry>,
    val notes: List<Note>,
    val codeDrafts: List<CodeDraft>,
    val mockInterviewSessions: List<MockInterviewSession>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupPreferences(
    val dailyGoal: Int,
    val notificationsEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val weeklySummaryEnabled: Boolean,
    val themeMode: String
)