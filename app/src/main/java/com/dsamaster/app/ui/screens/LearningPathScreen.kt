package com.dsamaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.viewmodel.LearningPathViewModel
import com.dsamaster.app.ui.viewmodel.LearningPathViewModelFactory
import com.dsamaster.app.ui.viewmodel.LessonListItem
import com.dsamaster.app.ui.viewmodel.ModuleListItem

@Composable
fun LearningPathScreen(onLessonClick: (Long) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: LearningPathViewModel = viewModel(
        factory = LearningPathViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingState(modifier = modifier, message = "Loading your learning path…")
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.pathName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Master the basics before problems and mock interviews",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            LearningPathProgressCard(
                completed = uiState.completedCount,
                total = uiState.totalCount,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        uiState.modules.forEach { module ->
            item(key = "module_${module.id}") {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
                )
            }
            items(module.lessons, key = { it.id }) { lesson ->
                LessonRow(lesson = lesson, onClick = { onLessonClick(lesson.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LearningPathProgressCard(completed: Int, total: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$completed of $total lessons complete",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else completed.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (total > 0 && completed == total) {
                    Text(
                        text = "Problems and Mock Interview are unlocked 🎉",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: LessonListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isLocked = lesson.status == "locked"
    val isCompleted = lesson.status == "completed"

    Card(
        onClick = { if (!isLocked) onClick() },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> SuccessGreen.copy(alpha = 0.15f)
                            isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Filled.CheckCircle
                        isLocked -> Icons.Filled.Lock
                        else -> Icons.Filled.PlayCircle
                    },
                    contentDescription = null,
                    tint = when {
                        isCompleted -> SuccessGreen
                        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = lesson.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isLocked) FontWeight.Normal else FontWeight.Medium,
                color = if (isLocked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}