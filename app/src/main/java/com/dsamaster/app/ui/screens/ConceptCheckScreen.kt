package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.components.LoadingState
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.viewmodel.ConceptCheckOption
import com.dsamaster.app.ui.viewmodel.ConceptCheckViewModel
import com.dsamaster.app.ui.viewmodel.ConceptCheckViewModelFactory

@Composable
fun ConceptCheckScreen(
    lessonId: Long,
    onPassed: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: ConceptCheckViewModel = viewModel(
        factory = ConceptCheckViewModelFactory(application, lessonId)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingState(modifier = modifier)
        return
    }

    if (uiState.isFinished) {
        ConceptCheckResult(
            correctCount = uiState.correctCount,
            total = uiState.questions.size,
            passed = uiState.passed,
            onContinue = onPassed,
            onRetry = { viewModel.onRetry() },
            modifier = modifier
        )
        return
    }

    val question = uiState.questions.getOrNull(uiState.currentIndex) ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Question ${uiState.currentIndex + 1} of ${uiState.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = question.question,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            question.options.forEach { option ->
                ConceptCheckOptionCard(
                    option = option,
                    isSelected = uiState.selectedOptionIndex == option.index,
                    isCorrect = option.index == question.correctOptionIndex,
                    hasAnswered = uiState.hasAnswered,
                    onClick = { viewModel.onOptionSelected(option.index) }
                )
            }
        }

        if (uiState.hasAnswered) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Button(
                onClick = { viewModel.onNext() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(if (uiState.currentIndex + 1 >= uiState.questions.size) "Finish" else "Next")
            }
        }
    }
}

@Composable
private fun ConceptCheckOptionCard(
    option: ConceptCheckOption,
    isSelected: Boolean,
    isCorrect: Boolean,
    hasAnswered: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !hasAnswered -> MaterialTheme.colorScheme.surface
        isCorrect -> SuccessGreen.copy(alpha = 0.15f)
        isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = { if (!hasAnswered) onClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Text(
            text = option.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun ConceptCheckResult(
    correctCount: Int,
    total: Int,
    passed: Boolean,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (passed) "Nice work!" else "Not quite there",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (passed) SuccessGreen else ErrorRed
        )
        Text(
            text = "You got $correctCount of $total correct.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (passed) {
            Button(onClick = onContinue, modifier = Modifier.padding(top = 24.dp)) {
                Text("Continue")
            }
        } else {
            Text(
                text = "Review the explanations above and give it another try.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
                Text("Retry")
            }
        }
    }
}