package com.dsamaster.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AuthGate(onAuthSuccess: () -> Unit) {
    var showLogin by remember { mutableStateOf(true) }

    if (showLogin) {
        LoginScreen(
            onAuthSuccess = onAuthSuccess,
            onNavigateToRegister = { showLogin = false }
        )
    } else {
        RegisterScreen(
            onAuthSuccess = onAuthSuccess,
            onNavigateToLogin = { showLogin = true }
        )
    }
}