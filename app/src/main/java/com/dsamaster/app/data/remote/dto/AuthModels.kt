package com.dsamaster.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class GoogleAuthRequestDto(
    val idToken: String
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    val name: String
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val user: AuthUserDto
)