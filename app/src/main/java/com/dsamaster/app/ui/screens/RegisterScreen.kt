package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Person
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
import com.dsamaster.app.ui.viewmodel.RegisterViewModel
import com.dsamaster.app.ui.viewmodel.RegisterViewModelFactory
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun RegisterScreen(
    onAuthSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(application))
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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
                text = "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your DSA progress across every session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
            )

            AuthTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name (optional)",
                leadingIcon = Icons.Rounded.Person,
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(14.dp))

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
                imeAction = ImeAction.Next
            )
            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm password",
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
                text = "Create account",
                loading = currentUiState is AuthUiState.Loading && !googleLoading,
                enabled = currentUiState !is AuthUiState.Loading && !googleLoading,
                onClick = {
                    validationError = validate(email, password, confirmPassword)
                    if (validationError == null) {
                        viewModel.register(
                            email = email.trim(),
                            password = password,
                            name = name.trim(),
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
                                viewModel.registerWithGoogle(idToken, onSuccess = onAuthSuccess)
                            }
                            .onFailure { error ->
                                validationError = error.message ?: "Google sign-in failed"
                            }
                        googleLoading = false
                    }
                }
            )

            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("Already have an account? Log in", textAlign = TextAlign.Center)
            }
        }
    }
}

private fun validate(email: String, password: String, confirmPassword: String): String? {
    return when {
        email.isBlank() || password.isBlank() -> "Email and password are required"
        !EMAIL_REGEX.matches(email) -> "Enter a valid email address"
        password.length < 6 -> "Password must be at least 6 characters"
        password != confirmPassword -> "Passwords don't match"
        else -> null
    }
}