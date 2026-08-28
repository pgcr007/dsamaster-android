package com.dsamaster.app.data.remote

import com.dsamaster.app.BuildConfig
import com.dsamaster.app.data.remote.dto.ErrorResponseDto
import com.dsamaster.app.data.remote.dto.HintResponse
import com.dsamaster.app.data.remote.dto.ReviewRequest
import com.dsamaster.app.data.remote.dto.ReviewResponse
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

// Result wrapper so the ViewModel doesn't need to deal with exceptions or
// figure out which response shape to expect - the two success cases are
// modeled explicitly since /review returns a different body for each mode.
sealed class ReviewResult {
    data class ReviewSuccess(val response: ReviewResponse) : ReviewResult()
    data class HintSuccess(val response: HintResponse) : ReviewResult()

    // retryable = true for failures a background retry could plausibly fix
    // (no connectivity, timeout, or a transient 5xx e.g. Render cold-starting).
    // retryable = false for failures that will just happen again with the same
    // payload (missing config, a 4xx from the backend, a decode error).
    data class Failure(val message: String, val retryable: Boolean = false) : ReviewResult()
}

class ReviewApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Groq inference is fast, but Render free-tier cold starts can still take
    // 30-60s on the first request after idle, same as /execute.
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun review(request: ReviewRequest): ReviewResult = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.BACKEND_BASE_URL
        val authToken = AuthTokenStore.token

        if (baseUrl.isBlank() || authToken.isNullOrBlank()) {
            return@withContext ReviewResult.Failure(
                "You're not logged in. Please log in again.",
                retryable = false
            )
        }

        val requestBodyJson = json.encodeToString(request)
        val httpRequest = Request.Builder()
            .url("$baseUrl/review")
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
                    // 5xx is usually transient (cold start, momentary overload) — worth
                    // a background retry. 4xx means the request itself is the problem.
                    return@withContext ReviewResult.Failure(errorMessage, retryable = response.code >= 500)
                }

                if (request.mode == "hint") {
                    val parsed = json.decodeFromString<HintResponse>(bodyString)
                    ReviewResult.HintSuccess(parsed)
                } else {
                    val parsed = json.decodeFromString<ReviewResponse>(bodyString)
                    ReviewResult.ReviewSuccess(parsed)
                }
            }
        } catch (e: IOException) {
            // Covers network errors, timeouts (e.g. Render free-tier cold start), no connectivity.
            ReviewResult.Failure(
                "Couldn't reach the review server. It may be waking up from sleep — try again in a moment.",
                retryable = true
            )
        } catch (e: Exception) {
            ReviewResult.Failure("Unexpected error: ${e.message}", retryable = false)
        }
    }
}