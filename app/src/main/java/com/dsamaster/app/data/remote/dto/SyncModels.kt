package com.dsamaster.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProgressSyncDto(
    val problemId: Long,
    val status: String,
    val lastAttemptDate: Long? = null,
    val timesReviewed: Int = 0,
    val nextReviewDate: Long? = null,
    val updatedAt: Long
)

@Serializable
data class StreakSyncDto(
    val date: String,
    val minutesActive: Int = 0,
    val problemsSolved: Int = 0,
    val streakFreezeUsed: Boolean = false,
    val updatedAt: Long
)

@Serializable
data class SyncPushRequestDto(
    val progress: List<ProgressSyncDto> = emptyList(),
    val streaks: List<StreakSyncDto> = emptyList()
)

@Serializable
data class SyncResponseDto(
    val progress: List<ProgressSyncDto> = emptyList(),
    val streaks: List<StreakSyncDto> = emptyList()
)