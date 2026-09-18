package com.dsamaster.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Topic as TopicIcon
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.entity.MockInterviewSession
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.repository.MockInterviewSessionRepository
import com.dsamaster.app.ui.components.EmptyState
import com.dsamaster.app.ui.components.InlineErrorCard
import com.dsamaster.app.ui.theme.DeepIndigo
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.theme.TealAccent
import com.dsamaster.app.ui.theme.WarningAmber
import com.dsamaster.app.ui.viewmodel.InterviewPhase
import com.dsamaster.app.ui.viewmodel.MockInterviewUiState
import com.dsamaster.app.ui.viewmodel.MockInterviewViewModel
import com.dsamaster.app.ui.viewmodel.MockInterviewViewModelFactory

private val DIFFICULTIES = listOf("Easy", "Medium", "Hard")
private val LANGUAGES = listOf("python" to "Python", "java" to "Java", "cpp" to "C++")

// The 4 active phases of an interview, in order. SETUP and SUMMARY sit outside this list.
private val INTERVIEW_STEPS: List<Pair<String, ImageVector>> = listOf(
    "Approach" to Icons.Filled.Lightbulb,
    "Clarify" to Icons.Filled.QuestionAnswer,
    "Code" to Icons.Filled.Code,
    "Follow-up" to Icons.Filled.Forum
)

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "Easy" -> SuccessGreen
    "Medium" -> WarningAmber
    "Hard" -> ErrorRed
    else -> TealAccent
}

// -1 = setup (no stepper), 0..3 = active step index, 4 = finished (all steps done)
private fun stepIndexFor(phase: InterviewPhase): Int = when (phase) {
    InterviewPhase.SETUP -> -1
    InterviewPhase.APPROACH -> 0
    InterviewPhase.CLARIFY -> 1
    InterviewPhase.CODING -> 2
    InterviewPhase.FOLLOWUP -> 3
    InterviewPhase.SUMMARY -> 4
}

@Composable
fun MockInterviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: MockInterviewViewModel = viewModel(
        factory = MockInterviewViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        MockInterviewTopBar(
            phase = uiState.phase,
            showingHistory = uiState.showHistory,
            onToggleHistory = viewModel::toggleHistory,
            onExitClick = { showExitDialog = true }
        )

        AnimatedVisibility(visible = !uiState.showHistory && stepIndexFor(uiState.phase) >= 0) {
            InterviewProgressStepper(
                phase = uiState.phase,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp)
            )
        }

        val viewKey = if (uiState.showHistory && uiState.phase == InterviewPhase.SETUP) {
            "history"
        } else {
            uiState.phase.name
        }

        AnimatedContent(
            targetState = viewKey,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) +
                        slideInHorizontally(animationSpec = tween(220)) { it / 8 })
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            modifier = Modifier.weight(1f),
            label = "interview-content"
        ) { key ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                when (key) {
                    "history" -> HistoryContent(uiState.pastSessions, viewModel::deleteSession)
                    InterviewPhase.SETUP.name -> SetupContent(uiState, viewModel)
                    InterviewPhase.APPROACH.name -> ApproachContent(uiState, viewModel)
                    InterviewPhase.CLARIFY.name -> ClarifyContent(uiState, viewModel)
                    InterviewPhase.CODING.name -> CodingContent(uiState, viewModel)
                    InterviewPhase.FOLLOWUP.name -> FollowUpContent(uiState, viewModel)
                    InterviewPhase.SUMMARY.name -> SummaryContent(uiState, viewModel)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showExitDialog) {
        ExitInterviewDialog(
            onConfirm = {
                showExitDialog = false
                viewModel.startNewInterview()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun MockInterviewTopBar(
    phase: InterviewPhase,
    showingHistory: Boolean,
    onToggleHistory: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Mock Interview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    showingHistory -> "Your past sessions"
                    phase == InterviewPhase.SETUP -> "Practice like it's the real thing"
                    phase == InterviewPhase.SUMMARY -> "Session wrapped up"
                    else -> "Interview in progress"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            phase == InterviewPhase.SETUP && !showingHistory -> {
                TextButton(onClick = onToggleHistory) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Past Sessions")
                }
            }
            phase == InterviewPhase.SETUP && showingHistory -> {
                TextButton(onClick = onToggleHistory) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Interview")
                }
            }
            phase != InterviewPhase.SUMMARY -> {
                IconButton(onClick = onExitClick) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit interview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitInterviewDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Close, contentDescription = null) },
        title = { Text("Exit this interview?") },
        text = { Text("Your progress on this session will be lost. This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Exit", color = ErrorRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep going") }
        }
    )
}

// ---------------------------------------------------------------------------
// Progress stepper
// ---------------------------------------------------------------------------

@Composable
private fun InterviewProgressStepper(phase: InterviewPhase, modifier: Modifier = Modifier) {
    val currentIndex = stepIndexFor(phase)
    if (currentIndex < 0) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            INTERVIEW_STEPS.forEachIndexed { index, (label, icon) ->
                val done = index < currentIndex
                val current = index == currentIndex
                val circleColor = if (done || current) TealAccent else MaterialTheme.colorScheme.surfaceVariant
                val iconTint = if (done || current) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (done) Icons.Filled.Check else icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (index != INTERVIEW_STEPS.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 4.dp)
                            .background(if (index < currentIndex) TealAccent else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (currentIndex >= INTERVIEW_STEPS.size) {
                "Interview complete"
            } else {
                "Step ${currentIndex + 1} of ${INTERVIEW_STEPS.size} \u00b7 ${INTERVIEW_STEPS[currentIndex].first}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
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

@Composable
private fun DifficultyFilterChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.18f),
            selectedLabelColor = color
        )
    )
}

@Composable
private fun InterviewHeader(problem: Problem, elapsedSeconds: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = problem.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                DifficultyBadge(difficulty = problem.difficulty)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = formatDuration(elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InterviewerMessageCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(TealAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Setup
// ---------------------------------------------------------------------------

@Composable
private fun HowItWorksCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(DeepIndigo, TealAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SupportAgent,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pick a topic and difficulty, or leave both open for a random pull from your problem bank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                INTERVIEW_STEPS.forEachIndexed { index, (label, icon) ->
                    FlowStepPill(label = label, icon = icon)
                    if (index != INTERVIEW_STEPS.lastIndex) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowStepPill(label: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TealAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SetupContent(uiState: MockInterviewUiState, viewModel: MockInterviewViewModel) {
    HowItWorksCard()

    SectionLabel(icon = Icons.Filled.Speed, text = "Difficulty")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = uiState.selectedDifficulty == null,
            onClick = { viewModel.onDifficultySelected(null) },
            label = { Text("Any") }
        )
        DIFFICULTIES.forEach { difficulty ->
            DifficultyFilterChip(
                label = difficulty,
                selected = uiState.selectedDifficulty == difficulty,
                color = difficultyColor(difficulty),
                onClick = {
                    viewModel.onDifficultySelected(
                        if (uiState.selectedDifficulty == difficulty) null else difficulty
                    )
                }
            )
        }
    }

    if (uiState.topics.isNotEmpty()) {
        SectionLabel(icon = Icons.Filled.TopicIcon, text = "Topic")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = uiState.selectedTopicId == null,
                    onClick = { viewModel.onTopicSelected(null) },
                    label = { Text("Any") }
                )
            }
            items(uiState.topics) { topic: Topic ->
                FilterChip(
                    selected = uiState.selectedTopicId == topic.id,
                    onClick = {
                        viewModel.onTopicSelected(
                            if (uiState.selectedTopicId == topic.id) null else topic.id
                        )
                    },
                    label = { Text(topic.name) }
                )
            }
        }
    }

    if (uiState.noMatchingProblems) {
        InlineErrorCard(message = "No problems match that combination. Try widening your filters.")
    }

    Button(
        onClick = viewModel::startInterview,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Text("  Start Interview", style = MaterialTheme.typography.titleSmall)
    }
}

// ---------------------------------------------------------------------------
// Approach
// ---------------------------------------------------------------------------

@Composable
private fun ApproachContent(uiState: MockInterviewUiState, viewModel: MockInterviewViewModel) {
    val problem = uiState.problem ?: return
    InterviewHeader(problem, uiState.elapsedSeconds)
    Text(
        text = "Before writing any code, explain your approach out loud - or here, in plain English. What's your plan?",
        style = MaterialTheme.typography.bodyMedium
    )
    OutlinedTextField(
        value = uiState.approachText,
        onValueChange = viewModel::onApproachChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        label = { Text("Your approach") },
        shape = RoundedCornerShape(14.dp)
    )
    uiState.clarifyError?.let { InlineErrorCard(message = it) }
    Button(
        onClick = viewModel::submitApproach,
        enabled = !uiState.isRequestingClarify && uiState.approachText.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (uiState.isRequestingClarify) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text("  Thinking...")
        } else {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Explain Approach")
        }
    }
}

// ---------------------------------------------------------------------------
// Clarify
// ---------------------------------------------------------------------------

@Composable
private fun ClarifyContent(uiState: MockInterviewUiState, viewModel: MockInterviewViewModel) {
    val problem = uiState.problem ?: return
    InterviewHeader(problem, uiState.elapsedSeconds)
    InterviewerMessageCard {
        Text(
            text = "Interviewer",
            style = MaterialTheme.typography.labelMedium,
            color = TealAccent,
            fontWeight = FontWeight.SemiBold
        )
        if (uiState.clarifyingAcknowledgement.isNotBlank()) {
            Text(text = uiState.clarifyingAcknowledgement, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = uiState.clarifyingQuestion,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
    OutlinedTextField(
        value = uiState.clarifyingAnswer,
        onValueChange = viewModel::onClarifyingAnswerChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        label = { Text("Your answer") },
        shape = RoundedCornerShape(14.dp)
    )
    Button(
        onClick = viewModel::proceedToCoding,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  Continue to Coding")
    }
}

// ---------------------------------------------------------------------------
// Coding
// ---------------------------------------------------------------------------

@Composable
private fun CodingContent(uiState: MockInterviewUiState, viewModel: MockInterviewViewModel) {
    val problem = uiState.problem ?: return
    InterviewHeader(problem, uiState.elapsedSeconds)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = problem.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        LANGUAGES.forEachIndexed { index, (key, label) ->
            SegmentedButton(
                selected = uiState.selectedLanguage == key,
                onClick = { viewModel.onLanguageSelected(key) },
                shape = SegmentedButtonDefaults.itemShape(index, LANGUAGES.size)
            ) {
                Text(label)
            }
        }
    }
    OutlinedTextField(
        value = uiState.currentCode,
        onValueChange = viewModel::onCodeChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        label = { Text("Your code") },
        shape = RoundedCornerShape(14.dp)
    )
    Text(
        text = "This is free-form - there's no test execution here, just like talking through code on a whiteboard. Run it for real afterward from the Problems tab.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    uiState.followUpError?.let { InlineErrorCard(message = it) }
    Button(
        onClick = viewModel::submitCode,
        enabled = !uiState.isRequestingFollowUp && uiState.currentCode.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (uiState.isRequestingFollowUp) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text("  Reviewing...")
        } else {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Submit Code")
        }
    }
}

// ---------------------------------------------------------------------------
// Follow-up
// ---------------------------------------------------------------------------

@Composable
private fun FollowUpContent(uiState: MockInterviewUiState, viewModel: MockInterviewViewModel) {
    val problem = uiState.problem ?: return
    InterviewHeader(problem, uiState.elapsedSeconds)
    InterviewerMessageCard {
        Text(
            text = "Interviewer",
            style = MaterialTheme.typography.labelMedium,
            color = TealAccent,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = uiState.followUpQuestion,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
    OutlinedTextField(
        value = uiState.followUpAnswer,
        onValueChange = viewModel::onFollowUpAnswerChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        label = { Text("Your answer") },
        shape = RoundedCornerShape(14.dp)
    )
    uiState.summaryError?.let { InlineErrorCard(message = it) }
    Button(
        onClick = viewModel::finishInterview,
        enabled = !uiState.isRequestingSummary && uiState.followUpAnswer.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (uiState.isRequestingSummary) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text("  Wrapping up...")
        } else {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Finish Interview")
        }
    }
}

// ---------------------------------------------------------------------------
// Summary
// ---------------------------------------------------------------------------

@Composable
private fun SummaryContent(uiState: MockInterviewUiState, viewModel: MockInterviewViewModel) {
    val problem = uiState.problem ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(DeepIndigo, TealAccent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Interview Complete",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = problem.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DifficultyBadge(difficulty = problem.difficulty)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = TealAccent.copy(alpha = 0.15f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatDuration(uiState.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = TealAccent,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }

    FeedbackCard(
        title = "Went well",
        points = uiState.wentWell,
        accentColor = SuccessGreen,
        backgroundAlpha = 0.1f,
        icon = Icons.Filled.CheckCircle
    )

    FeedbackCard(
        title = "Work on",
        points = uiState.workOn,
        accentColor = WarningAmber,
        backgroundAlpha = 0.12f,
        icon = Icons.Filled.ReportProblem
    )

    if (uiState.overallNotes.isNotBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Overall",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.overallNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    Button(
        onClick = viewModel::startNewInterview,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  Start New Interview")
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    points: List<String>,
    accentColor: Color,
    backgroundAlpha: Float,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = backgroundAlpha))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
            points.forEach { point ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------

@Composable
private fun HistoryContent(
    sessions: List<MockInterviewSession>,
    onDelete: (MockInterviewSession) -> Unit
) {
    if (sessions.isEmpty()) {
        EmptyState(
            title = "No past sessions yet",
            subtitle = "Finish an interview and it'll show up here.",
            modifier = Modifier.padding(top = 40.dp)
        )
        return
    }
    sessions.forEach { session ->
        SessionHistoryCard(session = session, onDelete = { onDelete(session) })
    }
}

@Composable
private fun SessionHistoryCard(
    session: MockInterviewSession,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.problemTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(difficulty = session.difficulty)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatDuration(session.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 3.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete session",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (session.overallNotes.isNotBlank()) {
                Text(text = session.overallNotes, style = MaterialTheme.typography.bodyMedium)
            }

            val wentWell = MockInterviewSessionRepository.decodeStringList(session.wentWellJson)
            val workOn = MockInterviewSessionRepository.decodeStringList(session.workOnJson)

            wentWell.forEach { point ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            workOn.forEach { point ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.ReportProblem,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}