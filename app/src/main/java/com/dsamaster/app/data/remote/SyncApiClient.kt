package com.dsamaster.app.data.remote

import com.dsamaster.app.BuildConfig
import com.dsamaster.app.data.remote.dto.ErrorResponseDto
import com.dsamaster.app.data.remote.dto.SyncPushRequestDto
import com.dsamaster.app.data.remote.dto.SyncResponseDto
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

sealed class SyncResult {
    data class Success(val data: SyncResponseDto) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

/**
 * Talks to the backend's /sync endpoint, which binds progress + streak data
 * to whichever account (Google or email/password) is currently signed in.
 *
 * IMPORTANT: the auth token is passed in explicitly by the caller rather
 * than read from AuthTokenStore inside this class. Pushes run on a
 * background coroutine that can sit queued for a while (Render free-tier
 * cold starts), and if it read the *global* token at execution time instead
 * of at call time, a push started under Account A could fire after the user
 * switched to Account B and get attributed to B's account on the server.
 * Capturing the token at the call site closes that race.
 */
class SyncApiClient {

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

    suspend fun pull(authToken: String): SyncResult = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.BACKEND_BASE_URL

        if (baseUrl.isBlank() || authToken.isBlank()) {
            return@withContext SyncResult.Failure("You're not logged in. Please log in again.")
        }

        val httpRequest = Request.Builder()
            .url("$baseUrl/sync")
            .addHeader("Authorization", "Bearer $authToken")
            .get()
            .build()

        executeSyncCall(httpRequest)
    }

    suspend fun push(payload: SyncPushRequestDto, authToken: String): SyncResult =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL

            if (baseUrl.isBlank() || authToken.isBlank()) {
                return@withContext SyncResult.Failure("You're not logged in. Please log in again.")
            }

            val requestBodyJson = json.encodeToString(payload)
            val httpRequest = Request.Builder()
                .url("$baseUrl/sync")
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            executeSyncCall(httpRequest)
        }

    private fun executeSyncCall(httpRequest: Request): SyncResult {
        return try {
            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMessage = try {
                        json.decodeFromString<ErrorResponseDto>(bodyString).error
                    } catch (e: Exception) {
                        "Sync failed with status ${response.code}"
                    }
                    return SyncResult.Failure(errorMessage)
                }

                val parsed = json.decodeFromString<SyncResponseDto>(bodyString)
                SyncResult.Success(parsed)
            }
        } catch (e: IOException) {
            SyncResult.Failure(
                "Couldn't reach the server to sync. It may be waking up from sleep."
            )
        } catch (e: Exception) {
            SyncResult.Failure("Unexpected sync error: ${e.message}")
        }
    }
}