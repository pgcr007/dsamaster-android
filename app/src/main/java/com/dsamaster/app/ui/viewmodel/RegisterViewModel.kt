package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.AuthApiClient
import com.dsamaster.app.data.remote.AuthResult
import com.dsamaster.app.data.remote.AuthTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class RegisterViewModel(
    private val userPreferences: UserPreferences,
    private val authApiClient: AuthApiClient = AuthApiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authApiClient.register(email, password, name)) {
                is AuthResult.Success -> {
                    userPreferences.setAuthSession(
                        token = result.token,
                        email = result.email,
                        name = result.name
                    )
                    AuthTokenStore.token = result.token
                    _uiState.value = AuthUiState.Idle
                    onSuccess()
                }
                is AuthResult.Failure -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun registerWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authApiClient.googleSignIn(idToken)) {
                is AuthResult.Success -> {
                    userPreferences.setAuthSession(
                        token = result.token,
                        email = result.email,
                        name = result.name
                    )
                    AuthTokenStore.token = result.token
                    _uiState.value = AuthUiState.Idle
                    onSuccess()
                }
                is AuthResult.Failure -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }
}