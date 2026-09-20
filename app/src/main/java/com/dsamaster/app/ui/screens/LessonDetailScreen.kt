package com.dsamaster.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.dsamaster.app.ui.components.TopicDiagram
import com.dsamaster.app.ui.theme.SuccessGreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontFamily
import com.dsamaster.app.ui.viewmodel.LessonDetailViewModel
import com.dsamaster.app.ui.viewmodel.LessonDetailViewModelFactory

@Composable
fun LessonDetailScreen(
    lessonId: Long,
    onTakeConceptCheck: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: LessonDetailViewModel = viewModel(
        factory = LessonDetailViewModelFactory(application, lessonId)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingState(modifier = modifier)
        return
    }

    // Defense in depth: the Learning Path screen never lets you tap a locked
    // lesson, but a stale deep link or back-stack restore shouldn't reveal it.
    if (uiState.status == "locked") {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Complete earlier lessons first",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (uiState.status == "completed") {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.height(18.dp)
                )
                Text(
                    text = "  Completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = SuccessGreen
                )
            }
        }

        if (uiState.diagramType != null) {
            TopicDiagram(
                diagramType = uiState.diagramType!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }

        Text(
            text = uiState.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )

        if (uiState.codeExample != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = uiState.codeExample!!,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (uiState.status == "unlocked") {
            if (uiState.hasConceptChecks) {
                Button(
                    onClick = onTakeConceptCheck,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text("Take Concept Check")
                }
            } else {
                Button(
                    onClick = { viewModel.markCompleteWithoutConceptCheck() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text("Mark as Complete")
                }
            }
        }
    }
}