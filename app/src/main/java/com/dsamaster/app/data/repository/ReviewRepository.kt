package com.dsamaster.app.data.repository

import com.dsamaster.app.data.remote.ReviewApiClient
import com.dsamaster.app.data.remote.ReviewResult
import com.dsamaster.app.data.remote.dto.ReviewRequest

class ReviewRepository(
    private val apiClient: ReviewApiClient = ReviewApiClient()
) {
    suspend fun requestReview(
        code: String,
        language: String,
        problemTitle: String,
        problemDescription: String,
        difficulty: String
    ): ReviewResult {
        val request = ReviewRequest(
            mode = "review",
            code = code,
            language = language,
            problemTitle = problemTitle,
            problemDescription = problemDescription,
            difficulty = difficulty
        )
        return apiClient.review(request)
    }

    suspend fun requestHint(
        code: String,
        language: String,
        problemTitle: String,
        problemDescription: String,
        difficulty: String,
        hintLevel: Int
    ): ReviewResult {
        val request = ReviewRequest(
            mode = "hint",
            code = code,
            language = language,
            problemTitle = problemTitle,
            problemDescription = problemDescription,
            difficulty = difficulty,
            hintLevel = hintLevel
        )
        return apiClient.review(request)
    }
}