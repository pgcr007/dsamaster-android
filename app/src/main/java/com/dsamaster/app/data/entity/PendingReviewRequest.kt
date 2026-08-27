package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A queued AI review/hint request that failed to reach the backend (offline,
 * timeout, or a transient 5xx) and is waiting to be retried automatically
 * once connectivity returns. See ReviewRetryWorker.
 */
@Entity(tableName = "pending_review_requests")
data class PendingReviewRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val problemId: Long,
    val problemTitle: String,
    val problemDescription: String,
    val difficulty: String,
    val code: String,
    val language: String,
    val mode: String, // "review" | "hint"
    val hintLevel: Int? = null,
    val createdAt: Long,
    val retryCount: Int = 0
)