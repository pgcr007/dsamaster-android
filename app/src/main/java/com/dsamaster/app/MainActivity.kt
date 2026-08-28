package com.dsamaster.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.AuthTokenStore
import com.dsamaster.app.ui.navigation.NavGraph
import com.dsamaster.app.ui.navigation.Screen
import com.dsamaster.app.ui.screens.AuthGate
import com.dsamaster.app.ui.screens.OnboardingScreen
import com.dsamaster.app.ui.theme.DsaMasterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: if denied, NotificationHelper's checks simply keep notifications silent */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        var startupDataLoaded = false
        splashScreen.setKeepOnScreenCondition { !startupDataLoaded }

        setContent {
            val application = applicationContext as DsaMasterApplication
            val themeMode by application.userPreferences.themeMode.collectAsState(
                initial = UserPreferences.DEFAULT_THEME_MODE
            )
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                UserPreferences.THEME_MODE_LIGHT -> false
                UserPreferences.THEME_MODE_DARK -> true
                else -> systemDark
            }

            var isReady by remember { mutableStateOf(false) }
            var showOnboardingFirst by remember { mutableStateOf(false) }
            var startedLoggedIn by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                showOnboardingFirst = !application.userPreferences.hasSeenOnboarding.first()
                val storedToken = application.userPreferences.authToken.first()
                startedLoggedIn = !storedToken.isNullOrBlank()
                AuthTokenStore.token = storedToken
                isReady = true
                startupDataLoaded = true
            }

            if (!isReady) {
                return@setContent
            }

            DsaMasterTheme(darkTheme = darkTheme) {
                DsaMasterRoot(
                    showOnboardingFirst = showOnboardingFirst,
                    startedLoggedIn = startedLoggedIn
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun DsaMasterRoot(showOnboardingFirst: Boolean, startedLoggedIn: Boolean) {
    var onboardingDone by remember { mutableStateOf(!showOnboardingFirst) }
    var loggedIn by remember { mutableStateOf(startedLoggedIn) }
    val application = LocalContext.current.applicationContext as DsaMasterApplication
    val scope = rememberCoroutineScope()

    when {
        !onboardingDone -> {
            OnboardingScreen(
                onFinished = {
                    scope.launch {
                        application.userPreferences.setHasSeenOnboarding(true)
                    }
                    onboardingDone = true
                }
            )
        }
        !loggedIn -> {
            AuthGate(onAuthSuccess = { loggedIn = true })
        }
        else -> {
            DsaMasterApp(onLogout = { loggedIn = false })
        }
    }
}

@Composable
fun DsaMasterApp(onLogout: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavGraph(navController = navController, innerPadding = innerPadding, onLogout = onLogout)
    }
}