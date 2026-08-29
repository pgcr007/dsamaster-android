package com.dsamaster.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.dsamaster.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Wraps the Credential Manager flow for "Sign in with Google". Must be called
 * with an Activity context (e.g. LocalContext.current inside a Composable
 * hosted by MainActivity) since it needs to show a system account picker.
 */
object GoogleSignInHelper {

    suspend fun requestIdToken(context: Context): Result<String> {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.success(credential.idToken)
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google sign-in was cancelled."))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Couldn't read the Google credential. Try again."))
        } catch (e: Exception) {
            Result.failure(Exception("Google sign-in failed: ${e.message}"))
        }
    }
}