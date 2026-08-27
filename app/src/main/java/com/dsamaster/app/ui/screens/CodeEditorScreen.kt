package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.remote.ExecuteResult
import com.dsamaster.app.data.remote.dto.ReviewResponse
import com.dsamaster.app.data.remote.dto.TestCaseResultDto
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.theme.TealAccent
import com.dsamaster.app.ui.theme.WarningAmber
import com.dsamaster.app.ui.viewmodel.CodeEditorViewModel
import com.dsamaster.app.ui.viewmodel.CodeEditorViewModelFactory
import com.dsamaster.app.ui.components.InlineErrorCard
import com.dsamaster.app.ui.components.LoadingState

private val languages = listOf("python" to "Python", "java" to "Java", "cpp" to "C++")

private const val INDENT_UNIT = "    " // 4 spaces
private val bracketPairs = mapOf('(' to ')', '[' to ']', '{' to '}')
private val quoteChars = setOf('"', '\'')
private val closingBrackets = setOf(')', ']', '}')

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
            Text(text = problem.title, style = MaterialTheme.typography.titleLarge)
        }

        if (uiState.isReviewMode) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "🧠 Review mode — solve this cold. Your saved draft won't load, and this attempt won't overwrite it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TealAccent,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
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
                label = { Text("Your code") }
            )
        }

        item {
            Button(
                onClick = viewModel::runCode,
                enabled = !uiState.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "  Running (server may need up to a minute to wake up)...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Run against test cases")
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
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isReviewing) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Get AI Review")
                    }
                }
                OutlinedButton(
                    onClick = viewModel::requestHint,
                    enabled = !uiState.isRequestingHint && uiState.hintLevel < 3,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isRequestingHint) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (uiState.hintLevel == 0) "I'm Stuck" else "More Help")
                    }
                }
            }
        }

        if (uiState.justMarkedSolved) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "🎉 All test cases passed — marked as solved, streak updated!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen,
                        modifier = Modifier.padding(16.dp)
                    )
                }
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
                item {
                    val passedCount = result.response.results.count { it.passed }
                    val total = result.response.results.size
                    Text(
                        text = "$passedCount / $total test cases passed",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(result.response.results) { testResult ->
                    TestCaseResultCard(testResult)
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun HintCard(level: Int, hint: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WarningAmber.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Hint $level / 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ReviewResultCard(review: ReviewResponse, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "AI Code Review",
                style = MaterialTheme.typography.titleMedium,
                color = TealAccent
            )
            ReviewSection(label = "Correctness", value = review.correctness)
            ReviewSection(label = "Time complexity", value = review.timeComplexity)
            ReviewSection(label = "Space complexity", value = review.spaceComplexity)
            ReviewSection(label = "Suggested improvement", value = review.improvement)
            ReviewSection(label = "Interviewer might ask", value = review.followUpQuestion)
        }
    }
}

@Composable
private fun ReviewSection(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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

@Composable
private fun TestCaseResultCard(result: TestCaseResultDto, modifier: Modifier = Modifier) {
    val color = if (result.passed) SuccessGreen else ErrorRed

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (result.passed) "Passed" else "Failed",
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = result.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Input: ${result.input}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Expected: ${result.expectedOutput}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Got: ${result.actualOutput}",
                style = MaterialTheme.typography.bodySmall
            )
            if (!result.stderr.isNullOrBlank()) {
                Text(
                    text = "Error: ${result.stderr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (!result.compileOutput.isNullOrBlank()) {
                Text(
                    text = "Compile error: ${result.compileOutput}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}