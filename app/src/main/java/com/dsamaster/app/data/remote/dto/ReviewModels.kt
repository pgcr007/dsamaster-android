package com.dsamaster.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body sent to POST /review on the backend.
 * mode = "review" for a full code review, or "hint" for a progressive hint.
 * hintLevel is only used when mode = "hint" (1, 2, or 3).
 */
@Serializable
data class ReviewRequest(
    val mode: String,
    val code: String,
    val language: String,
    val problemTitle: String,
    val problemDescription: String,
    val difficulty: String,
    val hintLevel: Int? = null
)

/**
 * Response for mode = "review". All fields are AI-generated feedback text.
 */
@Serializable
data class ReviewResponse(
    val mode: String,
    val correctness: String,
    val timeComplexity: String,
    val spaceComplexity: String,
    val improvement: String,
    val followUpQuestion: String
)

/**
 * Response for mode = "hint".
 */
@Serializable
data class HintResponse(
    val mode: String,
    val hintLevel: Int,
    val hint: String
)

/**
 * Generic error shape returned by the backend on 400/500.
 */
@Serializable
data class ReviewErrorResponse(
    val error: String,
    val detail: String? = null
)