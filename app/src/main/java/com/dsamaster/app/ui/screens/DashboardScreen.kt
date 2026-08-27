package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.components.StreakCounter
import com.dsamaster.app.ui.components.StreakHeatmap
import com.dsamaster.app.ui.viewmodel.DueReviewItem
import com.dsamaster.app.ui.viewmodel.ReviewQueueViewModel
import com.dsamaster.app.ui.viewmodel.ReviewQueueViewModelFactory
import com.dsamaster.app.ui.viewmodel.StreakViewModel
import com.dsamaster.app.ui.viewmodel.StreakViewModelFactory

@Composable
fun DashboardScreen(onReviewClick: (Long) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: StreakViewModel = viewModel(
        factory = StreakViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    val reviewQueueViewModel: ReviewQueueViewModel = viewModel(
        factory = ReviewQueueViewModelFactory(application)
    )
    val dueForReview by reviewQueueViewModel.dueForReview.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        item {
            StreakCounter(
                currentStreak = uiState.currentStreak,
                longestStreak = uiState.longestStreak,
                isAtRisk = uiState.isStreakAtRisk
            )
        }

        if (dueForReview.isNotEmpty()) {
            item {
                Text(
                    text = "Due for Review (${dueForReview.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(dueForReview, key = { it.problemId }) { due ->
                DueReviewCard(
                    due = due,
                    onReviewClick = { onReviewClick(due.problemId) }
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    StreakHeatmap(
                        entries = uiState.recentEntries,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Today's Pick",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Placeholder — real suggestions arrive once topics/problems exist (Phase 4/5)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.recordProblemSolved() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Simulate: Solve a problem today")
                }
                Button(
                    onClick = { viewModel.useStreakFreeze() },
                    enabled = uiState.canUseFreeze,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(if (uiState.canUseFreeze) "Use streak freeze" else "No freezes left this week")
                }
            }
        }
    }
}

@Composable
private fun DueReviewCard(
    due: DueReviewItem,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = due.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${due.difficulty} · reviewed ${due.timesReviewed}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onReviewClick) {
                Text("Review Now")
            }
        }
    }
}