package com.dsamaster.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request body sent to POST /interview on the backend.
 * mode = "clarify" | "followup" | "summary" - each mode uses a different
 * subset of the optional fields below (see backend/src/routes/interview.js).
 */
@Serializable
data class InterviewRequest(
    val mode: String,
    val problemTitle: String,
    val problemDescription: String? = null,
    val difficulty: String? = null,
    val approach: String? = null,
    val language: String? = null,
    val code: String? = null,
    val clarifyingQuestion: String? = null,
    val clarifyingAnswer: String? = null,
    val followUpQuestion: String? = null,
    val followUpAnswer: String? = null,
    val durationSeconds: Int? = null
)

/**
 * Response for mode = "clarify".
 */
@Serializable
data class ClarifyResponse(
    val mode: String,
    val question: String,
    val acknowledgement: String
)

/**
 * Response for mode = "followup".
 */
@Serializable
data class FollowUpResponse(
    val mode: String,
    val question: String
)

/**
 * Response for mode = "summary".
 */
@Serializable
data class SummaryResponse(
    val mode: String,
    val wentWell: List<String>,
    val workOn: List<String>,
    val overallNotes: String
)