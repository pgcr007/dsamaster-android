package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.R
import com.dsamaster.app.auth.GoogleSignInHelper
import com.dsamaster.app.ui.components.AuthBackdrop
import com.dsamaster.app.ui.components.AuthBrandMark
import com.dsamaster.app.ui.components.AuthOrDivider
import com.dsamaster.app.ui.components.AuthTextField
import com.dsamaster.app.ui.components.GradientAuthButton
import com.dsamaster.app.ui.components.InlineErrorCard
import com.dsamaster.app.ui.components.SocialSignInButton
import com.dsamaster.app.ui.viewmodel.AuthUiState
import com.dsamaster.app.ui.viewmodel.LoginViewModel
import com.dsamaster.app.ui.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(application))
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var googleLoading by remember { mutableStateOf(false) }

    AuthBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthBrandMark()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Log in to pick up where you left off.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 36.dp)
            )

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Rounded.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )

            val currentValidationError = validationError
            if (currentValidationError != null) {
                Spacer(modifier = Modifier.height(14.dp))
                InlineErrorCard(message = currentValidationError)
            }

            val currentUiState = uiState
            if (currentUiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(14.dp))
                InlineErrorCard(message = currentUiState.message)
            }

            Spacer(modifier = Modifier.height(28.dp))

            GradientAuthButton(
                text = "Log in",
                loading = currentUiState is AuthUiState.Loading && !googleLoading,
                enabled = currentUiState !is AuthUiState.Loading && !googleLoading,
                onClick = {
                    validationError = when {
                        email.isBlank() || password.isBlank() -> "Email and password are required"
                        else -> null
                    }
                    if (validationError == null) {
                        viewModel.login(
                            email = email.trim(),
                            password = password,
                            onSuccess = onAuthSuccess
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
            AuthOrDivider()
            Spacer(modifier = Modifier.height(20.dp))

            SocialSignInButton(
                text = "Continue with Google",
                iconRes = R.drawable.ic_google_logo,
                loading = googleLoading,
                enabled = currentUiState !is AuthUiState.Loading,
                onClick = {
                    coroutineScope.launch {
                        googleLoading = true
                        GoogleSignInHelper.requestIdToken(context)
                            .onSuccess { idToken ->
                                viewModel.loginWithGoogle(idToken, onSuccess = onAuthSuccess)
                            }
                            .onFailure { error ->
                                viewModel.setError(error.message ?: "Google sign-in failed")
                            }
                        googleLoading = false
                    }
                }
            )

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("Don't have an account? Create one", textAlign = TextAlign.Center)
            }
        }
    }
}