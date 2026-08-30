package com.dsamaster.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.viewmodel.TopicListItem
import com.dsamaster.app.ui.viewmodel.TopicViewModel
import com.dsamaster.app.ui.viewmodel.TopicViewModelFactory

@Composable
fun TopicsScreen(onTopicClick: (Long) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: TopicViewModel = viewModel(
        factory = TopicViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingState(modifier = modifier, message = "Loading topics…")
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
                text = "Topics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${uiState.totalTopics} topics across ${uiState.categories.size} categories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            TopicSearchField(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (uiState.searchQuery.isBlank()) {
            item {
                CategoryFilterRow(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (uiState.groupedTopics.isEmpty()) {
            item {
                NoTopicsFound(modifier = Modifier.padding(top = 32.dp))
            }
        }

        uiState.groupedTopics.forEach { (category, topics) ->
            item(key = "header_$category") {
                CategorySectionHeader(
                    category = category,
                    count = topics.size,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                )
            }
            items(topics, key = { it.id }) { topic ->
                TopicListCard(topic = topic, onClick = { onTopicClick(topic.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TopicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search topics…") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(if (selectedCategory == category) null else category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }
    }
}

@Composable
private fun CategorySectionHeader(category: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = category,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(start = 6.dp)
        )
        Text(
            text = " · $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TopicListCard(
    topic: TopicListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val difficultyColor = when (topic.difficultyLevel) {
        "Easy" -> MaterialTheme.colorScheme.tertiary
        "Medium" -> MaterialTheme.colorScheme.secondary
        "Hard" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(difficultyColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(topic.category),
                    contentDescription = null,
                    tint = difficultyColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 8.dp)
                    )
                    DifficultyChip(difficulty = topic.difficultyLevel, color = difficultyColor)
                }

                Text(
                    text = topic.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = " ${topic.timeComplexity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (topic.totalProblems > 0) {
                        Text(
                            text = "  •  ${topic.solvedProblems}/${topic.totalProblems} solved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (topic.totalProblems > 0) {
                    LinearProgressIndicator(
                        progress = { topic.solvedProblems.toFloat() / topic.totalProblems },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun NoTopicsFound(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "No topics found",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "Try a different search or category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Foundational" -> Icons.Filled.AutoAwesome
    "Linear Structures" -> Icons.Filled.ViewList
    "Non-Linear Structures" -> Icons.Filled.AccountTree
    "Algorithm Patterns" -> Icons.Filled.Extension
    "Arrays & Matrix Patterns" -> Icons.Filled.GridOn
    "String Algorithms" -> Icons.Filled.TextFields
    "Math for DSA" -> Icons.Filled.Functions
    "Design-Style Questions" -> Icons.Filled.Widgets
    else -> Icons.Filled.AutoAwesome
}