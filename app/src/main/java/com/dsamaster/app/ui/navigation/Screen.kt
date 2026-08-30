package com.dsamaster.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen(
        route = "dashboard",
        label = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Topics : Screen(
        route = "topics",
        label = "Topics",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )

    object Problems : Screen(
        route = "problems",
        label = "Problems",
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code
    )

    object MockInterview : Screen(
        route = "mock_interview",
        label = "Interview",
        selectedIcon = Icons.Filled.RecordVoiceOver,
        unselectedIcon = Icons.Outlined.RecordVoiceOver
    )

    object Settings : Screen(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    // Not part of the bottom nav — reached via the profile icon in the top bar.
    object Profile : Screen(
        route = "profile",
        label = "Profile",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Filled.AccountCircle
    )

    companion object {
        val bottomNavItems = listOf(Dashboard, Topics, Problems, MockInterview, Settings)
    }
}

object TopicDetailRoute {
    const val route = "topic_detail/{topicId}"
    fun createRoute(topicId: Long) = "topic_detail/$topicId"
}

object ProblemDetailRoute {
    const val route = "problem_detail/{problemId}"
    fun createRoute(problemId: Long) = "problem_detail/$problemId"
}

object ProblemsByTopicRoute {
    const val route = "problems_by_topic/{topicId}"
    fun createRoute(topicId: Long) = "problems_by_topic/$topicId"
}

object CodeEditorRoute {
    const val route = "code_editor/{problemId}?review={isReview}"
    fun createRoute(problemId: Long, isReview: Boolean = false) =
        "code_editor/$problemId?review=$isReview"
}