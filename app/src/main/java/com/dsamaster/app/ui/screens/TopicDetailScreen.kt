package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.components.TopicDiagram
import com.dsamaster.app.ui.viewmodel.TopicDetailViewModel
import com.dsamaster.app.ui.viewmodel.TopicDetailViewModelFactory

@Composable
fun TopicDetailScreen(
    topicId: Long,
    onPracticeProblemsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: TopicDetailViewModel = viewModel(
        factory = TopicDetailViewModelFactory(application, topicId)
    )
    val topic by viewModel.topic.collectAsState()

    val currentTopic = topic ?: run {
        LoadingState(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = currentTopic.name,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = currentTopic.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (currentTopic.diagramResId != null && currentTopic.diagramResId != "none") {
            item {
                Card(
                    onClick = onPracticeProblemsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Practice Problems",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text("→", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentTopic.explanation,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Complexity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text("Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentTopic.timeComplexity, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Space", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentTopic.spaceComplexity, style = MaterialTheme.typography.bodyMedium)
                        }
                        Column {
                            Text("Difficulty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentTopic.difficultyLevel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Asked at",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(currentTopic.companyTags.split(",")) { company ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = company.trim(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}