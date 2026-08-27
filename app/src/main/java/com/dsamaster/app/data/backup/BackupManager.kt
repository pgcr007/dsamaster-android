package com.dsamaster.app.data.backup

import android.content.Context
import android.net.Uri
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.repository.CodeDraftRepository
import com.dsamaster.app.data.repository.MockInterviewSessionRepository
import com.dsamaster.app.data.repository.NoteRepository
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed class BackupResult {
    data class ExportSuccess(val itemCount: Int) : BackupResult()
    data class RestoreSuccess(val itemCount: Int) : BackupResult()
    data class Failure(val message: String) : BackupResult()
}

class BackupManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val userProgressRepository: UserProgressRepository,
    private val streakRepository: StreakRepository,
    private val noteRepository: NoteRepository,
    private val codeDraftRepository: CodeDraftRepository,
    private val mockInterviewSessionRepository: MockInterviewSessionRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportTo(uri: Uri): BackupResult {
        return try {
            val bundle = buildBundle()
            val bytes = json.encodeToString(bundle).toByteArray()
            val opened = context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                true
            } ?: false

            if (!opened) return BackupResult.Failure("Couldn't open the selected file for writing.")

            BackupResult.ExportSuccess(bundle.itemCount())
        } catch (e: Exception) {
            BackupResult.Failure("Export failed: ${e.message}")
        }
    }

    suspend fun restoreFrom(uri: Uri): BackupResult {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: return BackupResult.Failure("Couldn't open the selected file for reading.")

            val bundle = json.decodeFromString<BackupBundle>(text)

            if (bundle.schemaVersion > BackupBundle.CURRENT_SCHEMA_VERSION) {
                return BackupResult.Failure(
                    "This backup was made with a newer version of the app and can't be restored here."
                )
            }

            bundle.userProgress.forEach { userProgressRepository.insertProgress(it) }
            bundle.streakEntries.forEach { streakRepository.insertStreakEntry(it) }
            bundle.notes.forEach { noteRepository.insertNote(it) }
            bundle.codeDrafts.forEach { codeDraftRepository.upsertDraft(it) }
            bundle.mockInterviewSessions.forEach { mockInterviewSessionRepository.insertSession(it) }

            userPreferences.setDailyGoal(bundle.preferences.dailyGoal)
            userPreferences.setNotificationsEnabled(bundle.preferences.notificationsEnabled)
            userPreferences.setReminderTime(bundle.preferences.reminderHour, bundle.preferences.reminderMinute)
            userPreferences.setWeeklySummaryEnabled(bundle.preferences.weeklySummaryEnabled)
            userPreferences.setThemeMode(bundle.preferences.themeMode)

            BackupResult.RestoreSuccess(bundle.itemCount())
        } catch (e: Exception) {
            BackupResult.Failure("Restore failed — file may not be a valid DSAMaster backup. (${e.message})")
        }
    }

    private suspend fun buildBundle(): BackupBundle {
        return BackupBundle(
            exportedAt = System.currentTimeMillis(),
            preferences = BackupPreferences(
                dailyGoal = userPreferences.dailyGoal.first(),
                notificationsEnabled = userPreferences.notificationsEnabled.first(),
                reminderHour = userPreferences.reminderHour.first(),
                reminderMinute = userPreferences.reminderMinute.first(),
                weeklySummaryEnabled = userPreferences.weeklySummaryEnabled.first(),
                themeMode = userPreferences.themeMode.first()
            ),
            userProgress = userProgressRepository.getAllProgress().first(),
            streakEntries = streakRepository.getAllStreakEntries().first(),
            notes = noteRepository.getAllNotes().first(),
            codeDrafts = codeDraftRepository.getAllDrafts().first(),
            mockInterviewSessions = mockInterviewSessionRepository.getAllSessions().first()
        )
    }

    private fun BackupBundle.itemCount(): Int =
        userProgress.size + streakEntries.size + notes.size + codeDrafts.size + mockInterviewSessions.size
}