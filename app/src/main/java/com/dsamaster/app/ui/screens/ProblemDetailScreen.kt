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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.seed.ExampleSeed
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.theme.TealAccent
import com.dsamaster.app.ui.theme.WarningAmber
import com.dsamaster.app.ui.viewmodel.ProblemDetailViewModel
import com.dsamaster.app.ui.viewmodel.ProblemDetailViewModelFactory
import kotlinx.serialization.json.Json

private val exampleJson = Json { ignoreUnknownKeys = true }

private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "Easy" -> SuccessGreen
    "Medium" -> WarningAmber
    "Hard" -> ErrorRed
    else -> TealAccent
}

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
            ProblemHeaderCard(
                title = problem.title,
                topicName = uiState.topicName,
                difficulty = problem.difficulty
            )
        }

        item {
            Button(
                onClick = onOpenEditorClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  Open Code Editor", style = MaterialTheme.typography.titleSmall)
            }
        }

        item {
            DescriptionCard(description = problem.description)
        }

        if (problem.constraints.isNotBlank()) {
            item {
                ConstraintsCard(constraints = problem.constraints)
            }
        }

        if (examples.isNotEmpty()) {
            item {
                SectionHeader(icon = Icons.Filled.Code, text = "Examples")
            }
            itemsIndexed(examples) { index, example ->
                ExampleCard(index = index + 1, example = example)
            }
        }

        if (hints.isNotEmpty()) {
            item {
                HintsSection(
                    hints = hints,
                    revealedCount = revealedHints,
                    onRevealMore = { revealedHints += 1 }
                )
            }
        }

        if (problem.companyTags.isNotBlank()) {
            item {
                SectionHeader(icon = Icons.Filled.Business, text = "Asked at")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(problem.companyTags.split(",")) { company ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = company.trim(),
                                style = MaterialTheme.typography.labelMedium,
                                color = TealAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TealAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun DifficultyBadge(difficulty: String, modifier: Modifier = Modifier) {
    val color = difficultyColor(difficulty)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun ProblemHeaderCard(
    title: String,
    topicName: String,
    difficulty: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (topicName.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Topic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = topicName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            DifficultyBadge(difficulty = difficulty)
        }
    }
}

// ---------------------------------------------------------------------------
// Description
// ---------------------------------------------------------------------------

@Composable
private fun DescriptionCard(description: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(icon = Icons.Filled.Description, text = "Description")
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Constraints
// ---------------------------------------------------------------------------

@Composable
private fun ConstraintsCard(constraints: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(icon = Icons.Filled.Checklist, text = "Constraints")
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                constraints.split("\n").forEach { line ->
                    if (line.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(TealAccent)
                            )
                            Text(
                                text = line.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Examples
// ---------------------------------------------------------------------------

@Composable
private fun ExampleCard(index: Int, example: ExampleSeed, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Example $index",
                style = MaterialTheme.typography.labelLarge,
                color = TealAccent,
                fontWeight = FontWeight.SemiBold
            )
            ExampleLine(label = "Input", value = example.input)
            ExampleLine(label = "Output", value = example.output)
            if (example.explanation.isNotBlank()) {
                Text(
                    text = example.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExampleLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ---------------------------------------------------------------------------
// Hints
// ---------------------------------------------------------------------------

@Composable
private fun HintsSection(
    hints: List<String>,
    revealedCount: Int,
    onRevealMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(icon = Icons.Filled.Lightbulb, text = "Hints")
        for (i in 0 until revealedCount) {
            HintRevealCard(index = i + 1, hint = hints[i])
        }
        if (revealedCount < hints.size) {
            OutlinedButton(
                onClick = onRevealMore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = "  Show hint ${revealedCount + 1} of ${hints.size}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun HintRevealCard(index: Int, hint: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "Hint $index",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}