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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.remote.ExecuteResult
import com.dsamaster.app.data.remote.dto.ReviewResponse
import com.dsamaster.app.data.remote.dto.TestCaseResultDto
import com.dsamaster.app.ui.components.InlineErrorCard
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.theme.TealAccent
import com.dsamaster.app.ui.theme.WarningAmber
import com.dsamaster.app.ui.viewmodel.CodeEditorViewModel
import com.dsamaster.app.ui.viewmodel.CodeEditorViewModelFactory

private val LANGUAGES = listOf("python" to "Python", "java" to "Java", "cpp" to "C++")

private const val INDENT_UNIT = "    " // 4 spaces
private val bracketPairs = mapOf('(' to ')', '[' to ']', '{' to '}')
private val quoteChars = setOf('"', '\'')
private val closingBrackets = setOf(')', ']', '}')

private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "Easy" -> SuccessGreen
    "Medium" -> WarningAmber
    "Hard" -> ErrorRed
    else -> TealAccent
}

// Central entry point: inspects the diff between old and new TextFieldValue
// and applies IDE-like behaviors (auto-indent, auto-dedent, bracket/quote
// pairing) on top of the default text-field edit. Falls back to returning
// `new` unmodified for anything more complex than a single-character edit
// (paste, multi-char replace, typing over a selection) so those still work
// exactly like a normal text field.
private fun applyEditorAssistance(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val oldCursorCollapsed = old.selection.start == old.selection.end
    val newCursorCollapsed = new.selection.start == new.selection.end
    if (!oldCursorCollapsed || !newCursorCollapsed) return new

    return when (new.text.length - old.text.length) {
        1 -> applyOnInsert(old, new)
        -1 -> applyOnBackspace(old, new)
        else -> new
    }
}

private fun applyOnInsert(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val cursor = new.selection.start
    val insertPos = cursor - 1
    if (insertPos < 0 || insertPos >= new.text.length) return new

    // Confirm this really was a plain insert at the cursor, not a replace.
    val expectedOldText = new.text.substring(0, insertPos) + new.text.substring(cursor)
    if (expectedOldText != old.text) return new

    val insertedChar = new.text[insertPos]

    if (insertedChar == '\n') {
        return applyAutoIndentOnNewline(new, cursor)
    }

    if (insertedChar in bracketPairs.keys) {
        val closing = bracketPairs.getValue(insertedChar)
        val newText = new.text.substring(0, cursor) + closing + new.text.substring(cursor)
        return new.copy(text = newText, selection = TextRange(cursor))
    }

    if (insertedChar in closingBrackets) {
        val nextChar = new.text.getOrNull(cursor)
        if (nextChar == insertedChar) {
            // Already sitting next to the matching closer we auto-inserted
            // earlier — skip over it instead of typing a duplicate.
            val newText = new.text.substring(0, insertPos) + new.text.substring(cursor)
            return new.copy(text = newText, selection = TextRange(cursor))
        }
        return new
    }

    if (insertedChar in quoteChars) {
        val nextChar = new.text.getOrNull(cursor)
        if (nextChar == insertedChar) {
            // Typing the closing quote right before an auto-inserted one: skip over it.
            val newText = new.text.substring(0, insertPos) + new.text.substring(cursor)
            return new.copy(text = newText, selection = TextRange(cursor))
        }
        val newText = new.text.substring(0, cursor) + insertedChar + new.text.substring(cursor)
        return new.copy(text = newText, selection = TextRange(cursor))
    }

    return new
}

private fun applyAutoIndentOnNewline(new: TextFieldValue, cursor: Int): TextFieldValue {
    val newlineIndex = cursor - 1
    val previousLineStart = new.text.lastIndexOf('\n', newlineIndex - 1) + 1
    val previousLine = new.text.substring(previousLineStart, newlineIndex)

    val currentIndent = previousLine.takeWhile { it == ' ' || it == '\t' }
    val trimmedPreviousLine = previousLine.trimEnd()
    val extraIndent = if (trimmedPreviousLine.endsWith(":") || trimmedPreviousLine.endsWith("{")) {
        INDENT_UNIT
    } else {
        ""
    }
    val indentToInsert = currentIndent + extraIndent
    if (indentToInsert.isEmpty()) return new

    val newText = new.text.substring(0, cursor) + indentToInsert + new.text.substring(cursor)
    val newCursor = cursor + indentToInsert.length
    return new.copy(text = newText, selection = TextRange(newCursor))
}

// Backspace inside leading whitespace jumps back to the previous 4-space
// tab stop in one keystroke, instead of deleting a single space at a time.
private fun applyOnBackspace(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val oldCursor = old.selection.start
    val newCursor = new.selection.start
    if (oldCursor - 1 != newCursor) return new

    val expectedNewText = old.text.substring(0, oldCursor - 1) + old.text.substring(oldCursor)
    if (expectedNewText != new.text) return new

    val deletedChar = old.text[oldCursor - 1]
    if (deletedChar != ' ') return new

    val lineStart = old.text.lastIndexOf('\n', oldCursor - 1) + 1
    val leadingBeforeDelete = old.text.substring(lineStart, oldCursor)
    if (leadingBeforeDelete.isEmpty() || leadingBeforeDelete.any { it != ' ' }) return new

    val currentIndentLen = leadingBeforeDelete.length
    val targetIndentLen = ((currentIndentLen - 1) / 4) * 4
    val additionalToRemove = (currentIndentLen - targetIndentLen) - 1
    if (additionalToRemove <= 0) return new

    val removeFrom = newCursor - additionalToRemove
    if (removeFrom < lineStart) return new

    val newText = new.text.substring(0, removeFrom) + new.text.substring(newCursor)
    return new.copy(text = newText, selection = TextRange(removeFrom))
}

@Composable
fun CodeEditorScreen(problemId: Long, isReviewMode: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: CodeEditorViewModel = viewModel(
        factory = CodeEditorViewModelFactory(application, problemId, isReviewMode)
    )
    val uiState by viewModel.uiState.collectAsState()
    val problem = uiState.problem ?: run {
        LoadingState(modifier = modifier)
        return
    }

    var textFieldValue by remember(uiState.selectedLanguage, uiState.problem != null) {
        mutableStateOf(TextFieldValue(uiState.currentCode))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProblemHeaderCard(problem = problem)
        }

        if (uiState.isReviewMode) {
            item {
                ReviewModeBanner()
            }
        }

        item {
            LanguageSelector(
                selected = uiState.selectedLanguage,
                onSelect = viewModel::onLanguageSelected
            )
        }

        item {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val adjusted = applyEditorAssistance(textFieldValue, newValue)
                    textFieldValue = adjusted
                    viewModel.onCodeChanged(adjusted.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                label = { Text("Your code") },
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (!uiState.isReviewMode) {
            item {
                Text(
                    text = "Autosaves as you type.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Button(
                onClick = viewModel::runCode,
                enabled = !uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "  Running (server may take up to a minute to wake up)...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  Run against test cases", style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::requestReview,
                    enabled = !uiState.isReviewing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isReviewing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  Get AI Review", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = viewModel::requestHint,
                    enabled = !uiState.isRequestingHint && uiState.hintLevel < 3,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isRequestingHint) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (uiState.hintLevel == 0) "  I'm Stuck" else "  More Help",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (uiState.justMarkedSolved) {
            item {
                JustSolvedCard()
            }
        }

        uiState.hintError?.let { message ->
            item {
                InlineErrorCard(message = message, onRetry = viewModel::requestHint)
            }
        }

        uiState.hintText?.let { hint ->
            item {
                HintCard(level = uiState.hintLevel, hint = hint)
            }
        }

        uiState.reviewError?.let { message ->
            item {
                InlineErrorCard(message = message, onRetry = viewModel::requestReview)
            }
        }

        uiState.reviewResult?.let { review ->
            item {
                ReviewResultCard(review)
            }
        }

        when (val result = uiState.executeResult) {
            is ExecuteResult.Failure -> {
                item {
                    InlineErrorCard(message = result.message, onRetry = viewModel::runCode)
                }
            }
            is ExecuteResult.Success -> {
                val passedCount = result.response.results.count { it.passed }
                val total = result.response.results.size
                item {
                    TestSummaryBar(passed = passedCount, total = total)
                }
                items(result.response.results) { testResult ->
                    TestCaseResultCard(testResult)
                }
            }
            null -> Unit
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun ProblemHeaderCard(problem: Problem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = problem.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            DifficultyBadge(difficulty = problem.difficulty)
        }
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
private fun ReviewModeBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                tint = TealAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Review mode — solve this cold. Your saved draft won't load, and this attempt won't overwrite it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TealAccent,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Language selector
// ---------------------------------------------------------------------------

@Composable
private fun LanguageSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        LANGUAGES.forEachIndexed { index, (key, label) ->
            SegmentedButton(
                selected = selected == key,
                onClick = { onSelect(key) },
                shape = SegmentedButtonDefaults.itemShape(index, LANGUAGES.size)
            ) {
                Text(label)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Solved celebration
// ---------------------------------------------------------------------------

@Composable
private fun JustSolvedCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "All test cases passed — marked as solved, streak updated!",
                style = MaterialTheme.typography.bodyMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hint
// ---------------------------------------------------------------------------

@Composable
private fun HintCard(level: Int, hint: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Hint",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarningAmber,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (index < level) WarningAmber else WarningAmber.copy(alpha = 0.25f))
                        )
                    }
                }
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AI review
// ---------------------------------------------------------------------------

@Composable
private fun ReviewResultCard(review: ReviewResponse, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "AI Code Review",
                    style = MaterialTheme.typography.titleMedium,
                    color = TealAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            ReviewSectionRow(icon = Icons.Filled.CheckCircle, label = "Correctness", value = review.correctness)
            ReviewSectionRow(icon = Icons.Filled.Speed, label = "Time complexity", value = review.timeComplexity)
            ReviewSectionRow(icon = Icons.Filled.Memory, label = "Space complexity", value = review.spaceComplexity)
            ReviewSectionRow(
                icon = Icons.Filled.TipsAndUpdates,
                label = "Suggested improvement",
                value = review.improvement
            )
            ReviewSectionRow(
                icon = Icons.Filled.QuestionAnswer,
                label = "Interviewer might ask",
                value = review.followUpQuestion
            )
        }
    }
}

@Composable
private fun ReviewSectionRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TealAccent,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Test results
// ---------------------------------------------------------------------------

@Composable
private fun TestSummaryBar(passed: Int, total: Int, modifier: Modifier = Modifier) {
    val ratio = if (total == 0) 0f else passed.toFloat() / total
    val color = when {
        total > 0 && passed == total -> SuccessGreen
        passed == 0 -> ErrorRed
        else -> WarningAmber
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$passed / $total test cases passed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (total > 0 && passed == total) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TestCaseResultCard(result: TestCaseResultDto, modifier: Modifier = Modifier) {
    val color = if (result.passed) SuccessGreen else ErrorRed

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.passed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (result.passed) "Passed" else "Failed",
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                    text = result.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TestCaseLine(label = "Input", value = result.input)
            TestCaseLine(label = "Expected", value = result.expectedOutput)
            TestCaseLine(label = "Got", value = result.actualOutput)
            if (!result.stderr.isNullOrBlank()) {
                TestCaseLine(label = "Error", value = result.stderr, valueColor = ErrorRed)
            }
            if (!result.compileOutput.isNullOrBlank()) {
                TestCaseLine(label = "Compile error", value = result.compileOutput, valueColor = ErrorRed)
            }
        }
    }
}

@Composable
private fun TestCaseLine(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}