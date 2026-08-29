package com.dsamaster.app.data.remote

import com.dsamaster.app.BuildConfig
import com.dsamaster.app.data.remote.dto.AuthResponseDto
import com.dsamaster.app.data.remote.dto.ErrorResponseDto
import com.dsamaster.app.data.remote.dto.GoogleAuthRequestDto
import com.dsamaster.app.data.remote.dto.LoginRequestDto
import com.dsamaster.app.data.remote.dto.RegisterRequestDto
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

sealed class AuthResult {
    data class Success(
        val token: String,
        val userId: String,
        val email: String,
        val name: String
    ) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthApiClient {

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

    suspend fun register(email: String, password: String, name: String): AuthResult =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            if (baseUrl.isBlank()) {
                return@withContext AuthResult.Failure(
                    "Backend URL is not configured. Check local.properties."
                )
            }

            val requestBodyJson = json.encodeToString(
                RegisterRequestDto(email = email, password = password, name = name)
            )
            val httpRequest = Request.Builder()
                .url("$baseUrl/auth/register")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            executeAuthCall(httpRequest)
        }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            if (baseUrl.isBlank()) {
                return@withContext AuthResult.Failure(
                    "Backend URL is not configured. Check local.properties."
                )
            }

            val requestBodyJson = json.encodeToString(LoginRequestDto(email = email, password = password))
            val httpRequest = Request.Builder()
                .url("$baseUrl/auth/login")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            executeAuthCall(httpRequest)
        }

    suspend fun googleSignIn(idToken: String): AuthResult =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            if (baseUrl.isBlank()) {
                return@withContext AuthResult.Failure(
                    "Backend URL is not configured. Check local.properties."
                )
            }

            val requestBodyJson = json.encodeToString(GoogleAuthRequestDto(idToken = idToken))
            val httpRequest = Request.Builder()
                .url("$baseUrl/auth/google")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            executeAuthCall(httpRequest)
        }

    private fun executeAuthCall(httpRequest: Request): AuthResult {
        return try {
            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMessage = try {
                        json.decodeFromString<ErrorResponseDto>(bodyString).error
                    } catch (e: Exception) {
                        "Request failed with status ${response.code}"
                    }
                    return AuthResult.Failure(errorMessage)
                }

                val parsed = json.decodeFromString<AuthResponseDto>(bodyString)
                AuthResult.Success(
                    token = parsed.token,
                    userId = parsed.user.id,
                    email = parsed.user.email,
                    name = parsed.user.name
                )
            }
        } catch (e: IOException) {
            AuthResult.Failure(
                "Couldn't reach the server. It may be waking up from sleep — try again in a moment."
            )
        } catch (e: Exception) {
            AuthResult.Failure("Unexpected error: ${e.message}")
        }
    }
}