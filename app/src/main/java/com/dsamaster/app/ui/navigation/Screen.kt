package com.dsamaster.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    object Topics : Screen("topics", "Topics", Icons.Filled.MenuBook)
    object Problems : Screen("problems", "Problems", Icons.Filled.Code)
    object MockInterview : Screen("mock_interview", "Mock Interview", Icons.Filled.RecordVoiceOver)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    // Not part of the bottom nav — reached via the profile icon in the top bar.
    object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)

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