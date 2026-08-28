package com.dsamaster.app.data.remote

import com.dsamaster.app.BuildConfig
import com.dsamaster.app.data.remote.dto.ErrorResponseDto
import com.dsamaster.app.data.remote.dto.ExecuteRequestDto
import com.dsamaster.app.data.remote.dto.ExecuteResponseDto
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

// Result wrapper so the ViewModel doesn't need to deal with exceptions directly.
sealed class ExecuteResult {
    data class Success(val response: ExecuteResponseDto) : ExecuteResult()
    data class Failure(val message: String) : ExecuteResult()
}

class CodeExecutionApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Judge0 cold starts + Render free-tier cold starts can both be slow,
    // so timeouts are generous (up to ~90s covers a worst-case double cold start).
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun execute(request: ExecuteRequestDto): ExecuteResult = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.BACKEND_BASE_URL
        val authToken = AuthTokenStore.token

        if (baseUrl.isBlank() || authToken.isNullOrBlank()) {
            return@withContext ExecuteResult.Failure(
                "You're not logged in. Please log in again."
            )
        }

        val requestBodyJson = json.encodeToString(request)
        val httpRequest = Request.Builder()
            .url("$baseUrl/execute")
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
                    return@withContext ExecuteResult.Failure(errorMessage)
                }

                val parsed = json.decodeFromString<ExecuteResponseDto>(bodyString)
                ExecuteResult.Success(parsed)
            }
        } catch (e: IOException) {
            // Covers network errors, timeouts (e.g. Render free-tier cold start), no connectivity.
            ExecuteResult.Failure(
                "Couldn't reach the code execution server. It may be waking up from sleep — try again in a moment."
            )
        } catch (e: Exception) {
            ExecuteResult.Failure("Unexpected error: ${e.message}")
        }
    }
}