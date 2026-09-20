package com.dsamaster.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.ui.viewmodel.LearningGateViewModel
import com.dsamaster.app.ui.viewmodel.LearningGateViewModelFactory

/**
 * Wraps Problem Bank / Mock Interview routes. Shows [content] once the
 * Foundations learning path is fully completed; otherwise shows a locked
 * placeholder with a CTA back to the Learning Path — enforced here so a
 * deep link (e.g. tapping a problem straight from the Dashboard) can't
 * bypass the gate.
 */
@Composable
fun LearningGate(
    onGoToLearningPath: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: LearningGateViewModel = viewModel(
        factory = LearningGateViewModelFactory(application)
    )
    val state by viewModel.gateState.collectAsState()

    when (val currentState = state) {
        is LearningGateViewModel.GateState.Loading -> LoadingState(modifier = modifier)
        is LearningGateViewModel.GateState.Unlocked -> content()
        is LearningGateViewModel.GateState.Locked -> LearningLockedState(
            completed = currentState.completed,
            total = currentState.total,
            onGoToLearningPath = onGoToLearningPath,
            modifier = modifier
        )
    }
}

@Composable
private fun LearningLockedState(
    completed: Int,
    total: Int,
    onGoToLearningPath: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Finish the Foundations first",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "You've completed $completed of $total foundational lessons. " +
                    "Finish them to unlock problems and mock interviews.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onGoToLearningPath, modifier = Modifier.padding(top = 20.dp)) {
            Text("Go to Learning Path")
        }
    }
}