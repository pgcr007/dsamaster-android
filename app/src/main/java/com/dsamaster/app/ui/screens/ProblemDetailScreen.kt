package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.seed.ExampleSeed
import com.dsamaster.app.ui.viewmodel.ProblemDetailViewModel
import com.dsamaster.app.ui.viewmodel.ProblemDetailViewModelFactory
import kotlinx.serialization.json.Json
import com.dsamaster.app.ui.components.LoadingState

private val exampleJson = Json { ignoreUnknownKeys = true }

@Composable
fun ProblemDetailScreen(
    problemId: Long,
    onOpenEditorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: ProblemDetailViewModel = viewModel(
        factory = ProblemDetailViewModelFactory(application, problemId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val problem = uiState.problem ?: run {
        LoadingState(modifier = modifier)
        return
    }

    val examples = remember(problem.examplesJson) {
        runCatching { exampleJson.decodeFromString<List<ExampleSeed>>(problem.examplesJson) }
            .getOrDefault(emptyList())
    }
    val hints = remember(problem.hints) {
        problem.hints?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    }
    var revealedHints by remember(problem.id) { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = problem.title, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = uiState.topicName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { DifficultyChip(difficulty = problem.difficulty) }

        item {
            Button(onClick = onOpenEditorClick, modifier = Modifier.fillMaxWidth()) {
                Text("Open Code Editor")
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = problem.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (problem.constraints.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Constraints", style = MaterialTheme.typography.titleMedium)
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            problem.constraints.split("\n").forEach { line ->
                                if (line.isNotBlank()) {
                                    Text(
                                        text = "•  $line",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (examples.isNotEmpty()) {
            item {
                Text(text = "Examples", style = MaterialTheme.typography.titleMedium)
            }
            items(examples) { example ->
                ExampleCard(example)
            }
        }

        if (hints.isNotEmpty()) {
            item {
                Column {
                    Text(text = "Hints", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        for (i in 0 until revealedHints) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "Hint ${i + 1}: ${hints[i]}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (revealedHints < hints.size) {
                            Button(onClick = { revealedHints += 1 }) {
                                Text("Show hint ${revealedHints + 1} of ${hints.size}")
                            }
                        }
                    }
                }
            }
        }

        if (problem.companyTags.isNotBlank()) {
            item {
                Text(text = "Asked at", style = MaterialTheme.typography.titleMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(problem.companyTags.split(",")) { company ->
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
}

@Composable
private fun ExampleCard(example: ExampleSeed, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Input: ${example.input}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Output: ${example.output}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (example.explanation.isNotBlank()) {
                Text(
                    text = example.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
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