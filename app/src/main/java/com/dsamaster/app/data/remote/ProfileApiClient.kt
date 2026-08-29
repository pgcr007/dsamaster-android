package com.dsamaster.app.data.remote

import com.dsamaster.app.BuildConfig
import com.dsamaster.app.data.remote.dto.ErrorResponseDto
import com.dsamaster.app.data.remote.dto.ProfileDto
import com.dsamaster.app.data.remote.dto.UpdateProfileRequestDto
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

sealed class ProfileResult {
    data class Success(val profile: ProfileDto) : ProfileResult()
    data class Failure(val message: String) : ProfileResult()
}

class ProfileApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Same cold-start allowance as the other API clients (Render free tier).
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getProfile(): ProfileResult = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.BACKEND_BASE_URL
        val authToken = AuthTokenStore.token

        if (baseUrl.isBlank() || authToken.isNullOrBlank()) {
            return@withContext ProfileResult.Failure("You're not logged in. Please log in again.")
        }

        val httpRequest = Request.Builder()
            .url("$baseUrl/profile/me")
            .addHeader("Authorization", "Bearer $authToken")
            .get()
            .build()

        executeProfileCall(httpRequest)
    }

    suspend fun updateProfile(request: UpdateProfileRequestDto): ProfileResult =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            val authToken = AuthTokenStore.token

            if (baseUrl.isBlank() || authToken.isNullOrBlank()) {
                return@withContext ProfileResult.Failure("You're not logged in. Please log in again.")
            }

            val requestBodyJson = json.encodeToString(request)
            val httpRequest = Request.Builder()
                .url("$baseUrl/profile/me")
                .addHeader("Authorization", "Bearer $authToken")
                .put(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            executeProfileCall(httpRequest)
        }

    private fun executeProfileCall(httpRequest: Request): ProfileResult {
        return try {
            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMessage = try {
                        json.decodeFromString<ErrorResponseDto>(bodyString).error
                    } catch (e: Exception) {
                        "Request failed with status ${response.code}"
                    }
                    return ProfileResult.Failure(errorMessage)
                }

                val parsed = json.decodeFromString<ProfileDto>(bodyString)
                ProfileResult.Success(parsed)
            }
        } catch (e: IOException) {
            ProfileResult.Failure(
                "Couldn't reach the server. It may be waking up from sleep — try again in a moment."
            )
        } catch (e: Exception) {
            ProfileResult.Failure("Unexpected error: ${e.message}")
        }
    }
}