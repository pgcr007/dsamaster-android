package com.dsamaster.app.data.remote

import com.dsamaster.app.BuildConfig
import com.dsamaster.app.data.remote.dto.ClarifyResponse
import com.dsamaster.app.data.remote.dto.ErrorResponseDto
import com.dsamaster.app.data.remote.dto.FollowUpResponse
import com.dsamaster.app.data.remote.dto.InterviewRequest
import com.dsamaster.app.data.remote.dto.SummaryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Three success shapes, one per mode, same pattern as ReviewResult.
sealed class InterviewResult {
    data class ClarifySuccess(val response: ClarifyResponse) : InterviewResult()
    data class FollowUpSuccess(val response: FollowUpResponse) : InterviewResult()
    data class SummarySuccess(val response: SummaryResponse) : InterviewResult()
    data class Failure(val message: String) : InterviewResult()
}

class InterviewApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Same cold-start allowance as ReviewApiClient/CodeExecutionApiClient.
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendInterviewRequest(request: InterviewRequest): InterviewResult =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            val authToken = BuildConfig.BACKEND_AUTH_TOKEN

            if (baseUrl.isBlank() || authToken.isBlank()) {
                return@withContext InterviewResult.Failure(
                    "Backend URL or auth token is not configured. Check local.properties."
                )
            }

            val requestBodyJson = json.encodeToString(request)
            val httpRequest = Request.Builder()
                .url("$baseUrl/interview")
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(httpRequest).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        val errorMessage = try {
                            json.decodeFromString<ErrorResponseDto>(bodyString).error
                        } catch (e: Exception) {
                            "Request failed with status ${response.code}"
                        }
                        return@withContext InterviewResult.Failure(errorMessage)
                    }

                    when (request.mode) {
                        "clarify" -> InterviewResult.ClarifySuccess(
                            json.decodeFromString<ClarifyResponse>(bodyString)
                        )
                        "followup" -> InterviewResult.FollowUpSuccess(
                            json.decodeFromString<FollowUpResponse>(bodyString)
                        )
                        "summary" -> InterviewResult.SummarySuccess(
                            json.decodeFromString<SummaryResponse>(bodyString)
                        )
                        else -> InterviewResult.Failure("Unknown interview mode: ${request.mode}")
                    }
                }
            } catch (e: IOException) {
                InterviewResult.Failure(
                    "Couldn't reach the interview server. It may be waking up from sleep - try again in a moment."
                )
            } catch (e: Exception) {
                InterviewResult.Failure("Unexpected error: ${e.message}")
            }
        }
}