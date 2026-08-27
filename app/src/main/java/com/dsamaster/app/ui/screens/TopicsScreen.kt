package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.viewmodel.TopicViewModel
import com.dsamaster.app.ui.viewmodel.TopicViewModelFactory

@Composable
fun TopicsScreen(onTopicClick: (Long) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: TopicViewModel = viewModel(
        factory = TopicViewModelFactory(application)
    )
    val topicsByCategory by viewModel.topicsByCategory.collectAsState()

    if (topicsByCategory.isEmpty()) {
        LoadingState(modifier = modifier, message = "Loading topics…")
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Topics",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        topicsByCategory.forEach { (category, topics) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(topics) { topic ->
                TopicCard(topic = topic, onClick = { onTopicClick(topic.id) })
            }
        }
    }
}

@Composable
private fun TopicCard(topic: Topic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Top
            ) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )
                DifficultyChip(difficulty = topic.difficultyLevel)
            }
            Text(
                text = topic.timeComplexity,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String, modifier: Modifier = Modifier) {
    val color = when (difficulty) {
        "Easy" -> MaterialTheme.colorScheme.tertiary
        "Medium" -> MaterialTheme.colorScheme.secondary
        "Hard" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}