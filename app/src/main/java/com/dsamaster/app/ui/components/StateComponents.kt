package com.dsamaster.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dsamaster.app.ui.theme.ErrorRed

/**
 * Full-space centered spinner for screens waiting on their first data emission
 * (e.g. a detail screen whose Room query hasn't resolved yet, or first-run seeding).
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * Centered placeholder for a list/collection with zero items — either because
 * nothing matches the current filter, or there's genuinely nothing there yet.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Inline error card matching the style already used for review/hint/execution
 * failures in the Code Editor — red-tinted surface, optional retry action.
 */
@Composable
fun InlineErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed
            )
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}