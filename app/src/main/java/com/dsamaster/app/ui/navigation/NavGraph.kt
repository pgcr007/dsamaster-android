package com.dsamaster.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dsamaster.app.ui.screens.CodeEditorScreen
import com.dsamaster.app.ui.screens.DashboardScreen
import com.dsamaster.app.ui.screens.MockInterviewScreen
import com.dsamaster.app.ui.screens.ProblemDetailScreen
import com.dsamaster.app.ui.screens.ProblemsScreen
import com.dsamaster.app.ui.screens.SettingsScreen
import com.dsamaster.app.ui.screens.TopicDetailScreen
import com.dsamaster.app.ui.screens.TopicsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onReviewClick = { problemId ->
                    navController.navigate(CodeEditorRoute.createRoute(problemId, isReview = true))
                }
            )
        }

        composable(Screen.Topics.route) {
            TopicsScreen(onTopicClick = { topicId ->
                navController.navigate(TopicDetailRoute.createRoute(topicId))
            })
        }

        composable(Screen.Problems.route) {
            ProblemsScreen(
                onProblemClick = { problemId ->
                    navController.navigate(ProblemDetailRoute.createRoute(problemId))
                }
            )
        }

        composable(Screen.MockInterview.route) { MockInterviewScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }

        composable(
            route = TopicDetailRoute.route,
            arguments = listOf(navArgument("topicId") { type = NavType.LongType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: 0L
            TopicDetailScreen(
                topicId = topicId,
                onPracticeProblemsClick = {
                    navController.navigate(ProblemsByTopicRoute.createRoute(topicId))
                }
            )
        }

        composable(
            route = ProblemsByTopicRoute.route,
            arguments = listOf(navArgument("topicId") { type = NavType.LongType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: 0L
            ProblemsScreen(
                initialTopicId = topicId,
                onProblemClick = { problemId ->
                    navController.navigate(ProblemDetailRoute.createRoute(problemId))
                }
            )
        }

        composable(
            route = ProblemDetailRoute.route,
            arguments = listOf(navArgument("problemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getLong("problemId") ?: 0L
            ProblemDetailScreen(
                problemId = problemId,
                onOpenEditorClick = {
                    navController.navigate(CodeEditorRoute.createRoute(problemId))
                }
            )
        }

        composable(
            route = CodeEditorRoute.route,
            arguments = listOf(
                navArgument("problemId") { type = NavType.LongType },
                navArgument("isReview") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getLong("problemId") ?: 0L
            val isReview = backStackEntry.arguments?.getBoolean("isReview") ?: false
            CodeEditorScreen(problemId = problemId, isReviewMode = isReview)
        }
    }
}