package com.dsamaster.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Full profile as returned by GET/PUT /profile/me. Mirrors the profile
 * fields on backend/src/models/User.js.
 */
@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val name: String = "",
    val bio: String = "",
    val targetRole: String = "",
    val targetCompanies: List<String> = emptyList(),
    val experienceLevel: String = "",
    val preferredLanguage: String = "python",
    val githubHandle: String = "",
    val linkedinUrl: String = "",
    val interviewTargetDate: String? = null, // "yyyy-MM-dd", null if not set
    val authProvider: String = "local",
    val createdAt: String? = null // ISO-8601 instant
)

/**
 * PUT body — every field is optional so the app can send only what changed.
 * A field left out of the request body is left untouched server-side.
 */
@Serializable
data class UpdateProfileRequestDto(
    val name: String? = null,
    val bio: String? = null,
    val targetRole: String? = null,
    val targetCompanies: List<String>? = null,
    val experienceLevel: String? = null,
    val preferredLanguage: String? = null,
    val githubHandle: String? = null,
    val linkedinUrl: String? = null,
    val interviewTargetDate: String? = null // send "" to clear an existing date
)