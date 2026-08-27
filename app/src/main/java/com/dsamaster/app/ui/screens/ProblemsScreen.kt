package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.ui.viewmodel.ProblemListItem
import com.dsamaster.app.ui.viewmodel.ProblemViewModel
import com.dsamaster.app.ui.viewmodel.ProblemViewModelFactory
import com.dsamaster.app.ui.components.EmptyState

private val DIFFICULTIES = listOf("Easy", "Medium", "Hard")

@Composable
fun ProblemsScreen(
    initialTopicId: Long? = null,
    onProblemClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: ProblemViewModel = viewModel(
        factory = ProblemViewModelFactory(application, initialTopicId)
    )

    val problems by viewModel.filteredProblems.collectAsState()
    val topics by viewModel.topicsWithProblems.collectAsState()
    val companies by viewModel.availableCompanies.collectAsState()
    val filter by viewModel.filter.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Problems",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        item {
            Text(
                text = "Difficulty",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter.difficulty == null,
                        onClick = { viewModel.setDifficultyFilter(null) },
                        label = { Text("All") }
                    )
                }
                items(DIFFICULTIES) { difficulty ->
                    FilterChip(
                        selected = filter.difficulty == difficulty,
                        onClick = {
                            viewModel.setDifficultyFilter(
                                if (filter.difficulty == difficulty) null else difficulty
                            )
                        },
                        label = { Text(difficulty) }
                    )
                }
            }
        }

        if (topics.isNotEmpty()) {
            item {
                Text(
                    text = "Topic",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.topicId == null,
                            onClick = { viewModel.setTopicFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(topics) { topic: Topic ->
                        FilterChip(
                            selected = filter.topicId == topic.id,
                            onClick = {
                                viewModel.setTopicFilter(
                                    if (filter.topicId == topic.id) null else topic.id
                                )
                            },
                            label = { Text(topic.name) }
                        )
                    }
                }
            }
        }

        if (companies.isNotEmpty()) {
            item {
                Text(
                    text = "Company",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.company == null,
                            onClick = { viewModel.setCompanyFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(companies) { company ->
                        FilterChip(
                            selected = filter.company == company,
                            onClick = {
                                viewModel.setCompanyFilter(
                                    if (filter.company == company) null else company
                                )
                            },
                            label = { Text(company) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "${problems.size} problem${if (problems.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (problems.isEmpty()) {
            item {
                EmptyState(
                    title = "No problems match these filters",
                    subtitle = "Try clearing a filter to widen the results"
                )
            }
        } else {
            items(problems) { item ->
                ProblemCard(item = item, onClick = { onProblemClick(item.problem.id) })
            }
        }
    }
}

@Composable
private fun ProblemCard(item: ProblemListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.problem.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )
                DifficultyChip(difficulty = item.problem.difficulty)
            }
            Text(
                text = item.topicName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (item.problem.companyTags.isNotBlank()) {
                Text(
                    text = item.problem.companyTags.split(",").joinToString(" · ") { it.trim() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
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