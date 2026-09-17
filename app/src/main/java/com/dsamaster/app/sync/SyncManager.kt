package com.dsamaster.app.data.sync

import android.util.Log
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.entity.UserProgress
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.AuthTokenStore
import com.dsamaster.app.data.remote.SyncApiClient
import com.dsamaster.app.data.remote.SyncResult
import com.dsamaster.app.data.remote.dto.ProgressSyncDto
import com.dsamaster.app.data.remote.dto.StreakSyncDto
import com.dsamaster.app.data.remote.dto.SyncPushRequestDto
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private fun UserProgress.toDto() = ProgressSyncDto(
    problemId = problemId,
    status = status,
    lastAttemptDate = lastAttemptDate,
    timesReviewed = timesReviewed,
    nextReviewDate = nextReviewDate,
    updatedAt = updatedAt
)

private fun StreakEntry.toDto() = StreakSyncDto(
    date = date,
    minutesActive = minutesActive,
    problemsSolved = problemsSolved,
    streakFreezeUsed = streakFreezeUsed,
    updatedAt = updatedAt
)

/**
 * Binds locally-tracked progress and streak data to whichever account
 * (Google or email/password) is currently signed in, by syncing it against
 * the backend's /sync endpoint.
 *
 * TOKEN HANDLING: pushProgressAsync/pushStreakAsync capture
 * AuthTokenStore.token *synchronously*, before launching the background
 * coroutine — not inside it. A push can sit queued for a while (Render
 * free-tier cold starts run 30-50s+), and if the account is switched while
 * a push is still in flight, reading the token lazily at execution time
 * would attribute the OLD account's data to the NEW account on the server.
 * Capturing it at the call site (same call stack as the DB write) closes
 * that race.
 */
class SyncManager(
    private val userProgressRepository: UserProgressRepository,
    private val streakRepository: StreakRepository,
    private val userPreferences: UserPreferences,
    private val syncApiClient: SyncApiClient = SyncApiClient()
) {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun pushProgressAsync(progress: UserProgress) {
        val tokenAtCallTime = AuthTokenStore.token ?: return
        backgroundScope.launch {
            try {
                syncApiClient.push(
                    SyncPushRequestDto(progress = listOf(progress.toDto())),
                    authToken = tokenAtCallTime
                )
            } catch (e: Exception) {
                Log.w("SyncManager", "Background progress push failed", e)
            }
        }
    }

    fun pushStreakAsync(entry: StreakEntry) {
        val tokenAtCallTime = AuthTokenStore.token ?: return
        backgroundScope.launch {
            try {
                syncApiClient.push(
                    SyncPushRequestDto(streaks = listOf(entry.toDto())),
                    authToken = tokenAtCallTime
                )
            } catch (e: Exception) {
                Log.w("SyncManager", "Background streak push failed", e)
            }
        }
    }

    /**
     * Reconciles local Room data with [userId]'s cloud copy. [authToken] is
     * the token JUST issued for [userId] — passed explicitly rather than
     * re-read from AuthTokenStore, for the same reason described above.
     */
    suspend fun syncAfterLogin(userId: String, authToken: String) {
        try {
            val lastUserId = userPreferences.lastSyncedUserId.first()

            if (!lastUserId.isNullOrBlank() && lastUserId != userId) {
                userProgressRepository.clearAll()
                streakRepository.clearAll()
            }

            when (val pullResult = syncApiClient.pull(authToken)) {
                is SyncResult.Success -> {
                    mergeRemoteProgress(pullResult.data.progress)
                    mergeRemoteStreaks(pullResult.data.streaks)
                }
                is SyncResult.Failure -> {
                    Log.w("SyncManager", "Sync pull failed: ${pullResult.message}")
                }
            }

            val localProgress = userProgressRepository.getAllProgressOnce()
            val localStreaks = streakRepository.getAllStreakEntriesOnce()
            if (localProgress.isNotEmpty() || localStreaks.isNotEmpty()) {
                syncApiClient.push(
                    SyncPushRequestDto(
                        progress = localProgress.map { it.toDto() },
                        streaks = localStreaks.map { it.toDto() }
                    ),
                    authToken = authToken
                )
            }

            userPreferences.setLastSyncedUserId(userId)
        } catch (e: Exception) {
            Log.w("SyncManager", "syncAfterLogin failed", e)
        }
    }

    private suspend fun mergeRemoteProgress(remote: List<ProgressSyncDto>) {
        remote.forEach { dto ->
            val existing = userProgressRepository.getProgressForProblemOnce(dto.problemId)
            if (existing == null || dto.updatedAt >= existing.updatedAt) {
                userProgressRepository.upsertFromSync(
                    UserProgress(
                        id = existing?.id ?: 0,
                        problemId = dto.problemId,
                        status = dto.status,
                        lastAttemptDate = dto.lastAttemptDate,
                        timesReviewed = dto.timesReviewed,
                        nextReviewDate = dto.nextReviewDate,
                        updatedAt = dto.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun mergeRemoteStreaks(remote: List<StreakSyncDto>) {
        remote.forEach { dto ->
            val existing = streakRepository.getStreakEntryByDateOnce(dto.date)
            if (existing == null || dto.updatedAt >= existing.updatedAt) {
                streakRepository.upsertFromSync(
                    StreakEntry(
                        id = existing?.id ?: 0,
                        date = dto.date,
                        minutesActive = dto.minutesActive,
                        problemsSolved = dto.problemsSolved,
                        streakFreezeUsed = dto.streakFreezeUsed,
                        updatedAt = dto.updatedAt
                    )
                )
            }
        }
    }
}