package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.entity.MockInterviewSession
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.repository.MockInterviewSessionRepository
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.theme.TealAccent
import com.dsamaster.app.ui.theme.WarningAmber
import com.dsamaster.app.ui.viewmodel.InterviewPhase
import com.dsamaster.app.ui.viewmodel.MockInterviewUiState
import com.dsamaster.app.ui.viewmodel.MockInterviewViewModel
import com.dsamaster.app.ui.viewmodel.MockInterviewViewModelFactory

private val DIFFICULTIES = listOf("Easy", "Medium", "Hard")
private val languages = listOf("python" to "Python", "java" to "Java", "cpp" to "C++")

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun MockInterviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: MockInterviewViewModel = viewModel(
        factory = MockInterviewViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Mock Interview", style = MaterialTheme.typography.headlineLarge)
                if (uiState.phase == InterviewPhase.SETUP) {
                    TextButton(onClick = viewModel::toggleHistory) {
                        Text(if (uiState.showHistory) "New Interview" else "Past Sessions")
                    }
                }
            }
        }

        if (uiState.showHistory && uiState.phase == InterviewPhase.SETUP) {
            historyContent(uiState.pastSessions, onDelete = viewModel::deleteSession)
        } else {
            when (uiState.phase) {
                InterviewPhase.SETUP -> setupContent(uiState, viewModel)
                InterviewPhase.APPROACH -> approachContent(uiState, viewModel)
                InterviewPhase.CLARIFY -> clarifyContent(uiState, viewModel)
                InterviewPhase.CODING -> codingContent(uiState, viewModel)
                InterviewPhase.FOLLOWUP -> followUpContent(uiState, viewModel)
                InterviewPhase.SUMMARY -> summaryContent(uiState, viewModel)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.setupContent(
    uiState: MockInterviewUiState,
    viewModel: MockInterviewViewModel
) {
    item {
        Text(
            text = "Pick a topic and difficulty, or leave both open for a random pull from your problem bank. You'll explain your approach, answer a clarifying question, code it, then handle a follow-up - just like a real screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    selected = uiState.selectedDifficulty == null,
                    onClick = { viewModel.onDifficultySelected(null) },
                    label = { Text("Any") }
                )
            }
            items(DIFFICULTIES) { difficulty ->
                FilterChip(
                    selected = uiState.selectedDifficulty == difficulty,
                    onClick = {
                        viewModel.onDifficultySelected(
                            if (uiState.selectedDifficulty == difficulty) null else difficulty
                        )
                    },
                    label = { Text(difficulty) }
                )
            }
        }
    }

    if (uiState.topics.isNotEmpty()) {
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
    }

    if (uiState.noMatchingProblems) {
        item {
            Text(
                text = "No problems match that combination. Try widening your filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed
            )
        }
    }

    item {
        Button(
            onClick = viewModel::startInterview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Interview")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.approachContent(
    uiState: MockInterviewUiState,
    viewModel: MockInterviewViewModel
) {
    val problem = uiState.problem ?: return
    item { InterviewHeader(problem, uiState.elapsedSeconds) }
    item {
        Text(
            text = "Before writing any code, explain your approach out loud - or here, in plain English. What's your plan?",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    item {
        OutlinedTextField(
            value = uiState.approachText,
            onValueChange = viewModel::onApproachChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            label = { Text("Your approach") }
        )
    }
    if (uiState.clarifyError != null) {
        item {
            Text(text = uiState.clarifyError, style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
        }
    }
    item {
        Button(
            onClick = viewModel::submitApproach,
            enabled = !uiState.isRequestingClarify && uiState.approachText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isRequestingClarify) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("  Thinking...")
            } else {
                Text("Explain Approach")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.clarifyContent(
    uiState: MockInterviewUiState,
    viewModel: MockInterviewViewModel
) {
    val problem = uiState.problem ?: return
    item { InterviewHeader(problem, uiState.elapsedSeconds) }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Interviewer",
                    style = MaterialTheme.typography.labelMedium,
                    color = TealAccent
                )
                Text(text = uiState.clarifyingAcknowledgement, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = uiState.clarifyingQuestion,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
    item {
        OutlinedTextField(
            value = uiState.clarifyingAnswer,
            onValueChange = viewModel::onClarifyingAnswerChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            label = { Text("Your answer") }
        )
    }
    item {
        Button(
            onClick = viewModel::proceedToCoding,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to Coding")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.codingContent(
    uiState: MockInterviewUiState,
    viewModel: MockInterviewViewModel
) {
    val problem = uiState.problem ?: return
    item { InterviewHeader(problem, uiState.elapsedSeconds) }
    item {
        Text(text = problem.description, style = MaterialTheme.typography.bodyMedium)
    }
    item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            languages.forEachIndexed { index, (key, label) ->
                SegmentedButton(
                    selected = uiState.selectedLanguage == key,
                    onClick = { viewModel.onLanguageSelected(key) },
                    shape = SegmentedButtonDefaults.itemShape(index, languages.size)
                ) {
                    Text(label)
                }
            }
        }
    }
    item {
        OutlinedTextField(
            value = uiState.currentCode,
            onValueChange = viewModel::onCodeChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            label = { Text("Your code") }
        )
    }
    item {
        Text(
            text = "This is free-form - there's no test execution here, just like talking through code on a whiteboard. Run it for real afterward from the Problems tab.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (uiState.followUpError != null) {
        item {
            Text(text = uiState.followUpError, style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
        }
    }
    item {
        Button(
            onClick = viewModel::submitCode,
            enabled = !uiState.isRequestingFollowUp && uiState.currentCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isRequestingFollowUp) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("  Reviewing...")
            } else {
                Text("Submit Code")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.followUpContent(
    uiState: MockInterviewUiState,
    viewModel: MockInterviewViewModel
) {
    val problem = uiState.problem ?: return
    item { InterviewHeader(problem, uiState.elapsedSeconds) }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Interviewer",
                    style = MaterialTheme.typography.labelMedium,
                    color = TealAccent
                )
                Text(
                    text = uiState.followUpQuestion,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
    item {
        OutlinedTextField(
            value = uiState.followUpAnswer,
            onValueChange = viewModel::onFollowUpAnswerChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text("Your answer") }
        )
    }
    if (uiState.summaryError != null) {
        item {
            Text(text = uiState.summaryError, style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
        }
    }
    item {
        Button(
            onClick = viewModel::finishInterview,
            enabled = !uiState.isRequestingSummary && uiState.followUpAnswer.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isRequestingSummary) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("  Wrapping up...")
            } else {
                Text("Finish Interview")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.summaryContent(
    uiState: MockInterviewUiState,
    viewModel: MockInterviewViewModel
) {
    val problem = uiState.problem ?: return
    item {
        Text(
            text = "${problem.title} - ${formatDuration(uiState.elapsedSeconds)}",
            style = MaterialTheme.typography.titleLarge
        )
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Went well", style = MaterialTheme.typography.labelLarge, color = SuccessGreen)
                uiState.wentWell.forEach { point ->
                    Text(text = "• $point", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Work on", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                uiState.workOn.forEach { point ->
                    Text(text = "• $point", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    item {
        Text(text = uiState.overallNotes, style = MaterialTheme.typography.bodyMedium)
    }
    item {
        Button(
            onClick = viewModel::startNewInterview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start New Interview")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historyContent(
    sessions: List<MockInterviewSession>,
    onDelete: (MockInterviewSession) -> Unit
) {
    if (sessions.isEmpty()) {
        item {
            Text(
                text = "No past sessions yet. Finish an interview and it'll show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    items(sessions) { session ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = session.problemTitle, style = MaterialTheme.typography.titleMedium)
                    Text(text = session.difficulty, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = "${formatDuration(session.durationSeconds)} spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = session.overallNotes, style = MaterialTheme.typography.bodyMedium)
                val wentWell = MockInterviewSessionRepository.decodeStringList(session.wentWellJson)
                val workOn = MockInterviewSessionRepository.decodeStringList(session.workOnJson)
                wentWell.forEach { Text(text = "+ $it", style = MaterialTheme.typography.bodySmall, color = SuccessGreen) }
                workOn.forEach { Text(text = "- $it", style = MaterialTheme.typography.bodySmall, color = WarningAmber) }
                OutlinedButton(
                    onClick = { onDelete(session) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun InterviewHeader(problem: Problem, elapsedSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = problem.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = problem.difficulty,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatDuration(elapsedSeconds),
            style = MaterialTheme.typography.titleMedium,
            color = TealAccent
        )
    }
}