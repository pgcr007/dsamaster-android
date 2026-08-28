package com.dsamaster.app.data.remote

/**
 * In-memory holder for the current user's JWT, read by every authenticated
 * API client (CodeExecutionApiClient, ReviewApiClient, InterviewApiClient -
 * wired in Phase 13 Step 10). Kept in sync with the persisted copy in
 * UserPreferences: loaded once at app startup, updated on login/register,
 * cleared on logout.
 */
object AuthTokenStore {
    @Volatile
    var token: String? = null
}