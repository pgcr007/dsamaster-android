package com.dsamaster.app.data.repository

import com.dsamaster.app.data.remote.InterviewApiClient
import com.dsamaster.app.data.remote.InterviewResult
import com.dsamaster.app.data.remote.dto.InterviewRequest

class InterviewRepository(
    private val apiClient: InterviewApiClient = InterviewApiClient()
) {
    suspend fun requestClarifyingQuestion(
        problemTitle: String,
        problemDescription: String,
        difficulty: String,
        approach: String
    ): InterviewResult {
        val request = InterviewRequest(
            mode = "clarify",
            problemTitle = problemTitle,
            problemDescription = problemDescription,
            difficulty = difficulty,
            approach = approach
        )
        return apiClient.sendInterviewRequest(request)
    }

    suspend fun requestFollowUpQuestion(
        problemTitle: String,
        problemDescription: String,
        difficulty: String,
        approach: String,
        language: String,
        code: String
    ): InterviewResult {
        val request = InterviewRequest(
            mode = "followup",
            problemTitle = problemTitle,
            problemDescription = problemDescription,
            difficulty = difficulty,
            approach = approach,
            language = language,
            code = code
        )
        return apiClient.sendInterviewRequest(request)
    }

    suspend fun requestSessionSummary(
        problemTitle: String,
        difficulty: String,
        approach: String,
        clarifyingQuestion: String,
        clarifyingAnswer: String,
        language: String,
        code: String,
        followUpQuestion: String,
        followUpAnswer: String,
        durationSeconds: Int
    ): InterviewResult {
        val request = InterviewRequest(
            mode = "summary",
            problemTitle = problemTitle,
            difficulty = difficulty,
            approach = approach,
            language = language,
            code = code,
            clarifyingQuestion = clarifyingQuestion,
            clarifyingAnswer = clarifyingAnswer,
            followUpQuestion = followUpQuestion,
            followUpAnswer = followUpAnswer,
            durationSeconds = durationSeconds
        )
        return apiClient.sendInterviewRequest(request)
    }
}