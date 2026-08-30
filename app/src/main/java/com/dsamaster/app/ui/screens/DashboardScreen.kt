package com.dsamaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.BuildConfig
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.components.StreakHeatmap
import com.dsamaster.app.ui.theme.DeepIndigo
import com.dsamaster.app.ui.theme.IndigoLight
import com.dsamaster.app.ui.theme.TealAccent
import com.dsamaster.app.ui.viewmodel.ContinueLearningItem
import com.dsamaster.app.ui.viewmodel.DashboardViewModel
import com.dsamaster.app.ui.viewmodel.DashboardViewModelFactory
import com.dsamaster.app.ui.viewmodel.DifficultyProgress
import com.dsamaster.app.ui.viewmodel.DueReviewItem
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onReviewClick: (Long) -> Unit = {},
    onProblemClick: (Long) -> Unit = {},
    onBrowseTopics: () -> Unit = {},
    onBrowseProblems: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        DashboardHeader()
        Spacer(modifier = Modifier.height(16.dp))

        StreakHeroCard(
            currentStreak = uiState.currentStreak,
            longestStreak = uiState.longestStreak,
            isAtRisk = uiState.isStreakAtRisk,
            problemsSolvedToday = uiState.problemsSolvedToday,
            dailyGoal = uiState.dailyGoal,
            goalProgress = uiState.goalProgress,
            canUseFreeze = uiState.canUseFreeze,
            onUseFreeze = { viewModel.useStreakFreeze() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        ProgressOverviewCard(
            solvedProblems = uiState.solvedProblems,
            totalProblems = uiState.totalProblems,
            overallPercent = uiState.overallProgressPercent,
            difficultyBreakdown = uiState.difficultyBreakdown
        )

        Spacer(modifier = Modifier.height(14.dp))

        ContinueLearningCard(
            item = uiState.continueLearning,
            onClick = { item -> onProblemClick(item.problemId) }
        )

        if (uiState.dueForReview.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(title = "Due for Review", count = uiState.dueForReview.size)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.dueForReview, key = { it.problemId }) { due ->
                    DueReviewCard(due = due, onReviewClick = { onReviewClick(due.problemId) })
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Activity")
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Last 26 weeks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StreakHeatmap(
                    entries = uiState.recentEntries,
                    modifier = Modifier.padding(top = 10.dp)
                )
                HeatmapLegend(modifier = Modifier.padding(top = 6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SectionHeader(title = "Quick Actions")
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "Topics",
                icon = Icons.Filled.MenuBook,
                modifier = Modifier.weight(1f),
                onClick = onBrowseTopics
            )
            QuickActionButton(
                label = "Problems",
                icon = Icons.Filled.Code,
                modifier = Modifier.weight(1f),
                onClick = onBrowseProblems
            )
        }

        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(20.dp))
            DebugToolsSection(
                canUseFreeze = uiState.canUseFreeze,
                onSimulateSolve = { viewModel.recordProblemSolved() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DashboardHeader(modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val dateText = remember(today) {
        val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$dayName, $monthName ${today.dayOfMonth}"
    }
    val greeting = remember { timeOfDayGreeting() }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun timeOfDayGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun StreakHeroCard(
    currentStreak: Int,
    longestStreak: Int,
    isAtRisk: Boolean,
    problemsSolvedToday: Int,
    dailyGoal: Int,
    goalProgress: Float,
    canUseFreeze: Boolean,
    onUseFreeze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(DeepIndigo, IndigoLight, TealAccent))
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "$currentStreak",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "day streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " Best",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = "$longestStreak",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isAtRisk) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " Solve today to keep your streak alive",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's goal",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "$problemsSolvedToday / $dailyGoal solved",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .height(8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            TextButton(
                onClick = onUseFreeze,
                enabled = canUseFreeze,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = if (canUseFreeze) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (canUseFreeze) " Use streak freeze" else " No freezes left this week",
                    color = if (canUseFreeze) Color.White else Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ProgressOverviewCard(
    solvedProblems: Int,
    totalProblems: Int,
    overallPercent: Int,
    difficultyBreakdown: List<DifficultyProgress>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$solvedProblems / $totalProblems",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { if (totalProblems <= 0) 0f else solvedProblems.toFloat() / totalProblems },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .height(8.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "$overallPercent% complete across all topics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (difficultyBreakdown.any { it.total > 0 }) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    difficultyBreakdown.forEach { bucket ->
                        DifficultyStat(bucket = bucket, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyStat(bucket: DifficultyProgress, modifier: Modifier = Modifier) {
    val color = when (bucket.label) {
        "Easy" -> MaterialTheme.colorScheme.tertiary
        "Medium" -> MaterialTheme.colorScheme.secondary
        "Hard" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = modifier) {
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${bucket.solved}/${bucket.total}",
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
        LinearProgressIndicator(
            progress = { if (bucket.total <= 0) 0f else bucket.solved.toFloat() / bucket.total },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun ContinueLearningCard(
    item: ContinueLearningItem?,
    onClick: (ContinueLearningItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item != null) { item?.let(onClick) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(DeepIndigo, TealAccent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "Continue Learning",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item?.title ?: "You're all caught up!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = item?.let { "${it.topicName} · ${it.difficulty}" }
                        ?: "New problems will show up here once added",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (item != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Start",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (count != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
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
    val color = when (due.difficulty) {
        "Easy" -> MaterialTheme.colorScheme.tertiary
        "Medium" -> MaterialTheme.colorScheme.secondary
        "Hard" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .width(220.dp)
            .clickable { onReviewClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = due.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = due.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Reviewed ${due.timesReviewed}x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(
                onClick = onReviewClick,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text("Review Now")
            }
        }
    }
}

@Composable
private fun HeatmapLegend(modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(0.15f, 0.4f, 0.7f, 1f).forEach { alpha ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor.copy(alpha = alpha))
            )
        }
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(52.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = " $label", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun DebugToolsSection(
    canUseFreeze: Boolean,
    onSimulateSolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Debug tools (visible in debug builds only)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onSimulateSolve, modifier = Modifier.padding(top = 4.dp)) {
                Text("Simulate: solve a problem today")
            }
        }
    }
}