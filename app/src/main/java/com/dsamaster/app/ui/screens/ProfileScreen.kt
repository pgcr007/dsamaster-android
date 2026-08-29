@file:OptIn(ExperimentalMaterial3Api::class)

package com.dsamaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.remote.dto.ProfileDto
import com.dsamaster.app.ui.theme.ErrorRed
import com.dsamaster.app.ui.theme.SuccessGreen
import com.dsamaster.app.ui.theme.WarningAmber
import com.dsamaster.app.ui.viewmodel.ProfileEditState
import com.dsamaster.app.ui.viewmodel.ProfileStats
import com.dsamaster.app.ui.viewmodel.ProfileUiState
import com.dsamaster.app.ui.viewmodel.ProfileViewModel
import com.dsamaster.app.ui.viewmodel.ProfileViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val TARGET_ROLES = listOf("SDE-1", "SDE-2", "SDE-3", "Senior SDE", "ML Engineer", "Other")
private val EXPERIENCE_LEVELS = listOf("Student", "0-1 years", "1-3 years", "3-5 years", "5+ years")
private val TARGET_COMPANIES = listOf(
    "Google", "Amazon", "Microsoft", "Meta", "Apple", "Netflix",
    "Adobe", "Uber", "Flipkart", "PhonePe", "Goldman Sachs", "Atlassian"
)

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(application))

    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.isLoading && uiState.profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            ProfileHeader(uiState = uiState)

            Spacer(modifier = Modifier.height(20.dp))

            StatsSection(stats = stats)

            uiState.profile?.interviewTargetDate?.let { dateStr ->
                Spacer(modifier = Modifier.height(16.dp))
                InterviewCountdownCard(dateStr)
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            uiState.errorMessage?.let { message ->
                ErrorBanner(message) { viewModel.clearMessages() }
            }
            uiState.infoMessage?.let { message ->
                InfoBanner(message) { viewModel.clearMessages() }
            }

            if (!uiState.isEditing) {
                ProfileDetailsReadOnly(uiState.profile)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.startEditing() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit profile")
                }
            } else {
                ProfileEditForm(
                    draft = uiState.draft,
                    isSaving = uiState.isSaving,
                    onDraftChange = { viewModel.updateDraft(it) },
                    onToggleCompany = { viewModel.toggleTargetCompany(it) },
                    onSave = { viewModel.save() },
                    onCancel = { viewModel.cancelEditing() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileHeader(uiState: ProfileUiState) {
    val profile = uiState.profile

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsFor(profile?.name.orEmpty()),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = profile?.name?.takeIf { it.isNotBlank() } ?: "Your profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = profile?.email.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!profile?.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = profile?.bio.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        profile?.let {
            if (it.targetRole.isNotBlank()) {
                AssistChip(onClick = {}, label = { Text(it.targetRole) })
            }
            if (it.experienceLevel.isNotBlank()) {
                AssistChip(onClick = {}, label = { Text(it.experienceLevel) })
            }
            if (it.authProvider != "local") {
                AssistChip(
                    onClick = {},
                    label = { Text("Signed in with ${it.authProvider.replaceFirstChar { c -> c.uppercase() }}") }
                )
            }
            memberSinceLabel(it.createdAt)?.let { label ->
                AssistChip(onClick = {}, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun StatsSection(stats: ProfileStats) {
    Text(text = "Your progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(modifier = Modifier.weight(1f), value = "${stats.problemsSolved}/${stats.totalProblems}", label = "Solved")
        StatCard(modifier = Modifier.weight(1f), value = stats.currentStreak.toString(), label = "Day streak")
        StatCard(modifier = Modifier.weight(1f), value = stats.longestStreak.toString(), label = "Best streak")
    }

    Spacer(modifier = Modifier.height(12.dp))

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DifficultyChip(label = "Easy", count = stats.easySolved, color = SuccessGreen)
        DifficultyChip(label = "Medium", count = stats.mediumSolved, color = WarningAmber)
        DifficultyChip(label = "Hard", count = stats.hardSolved, color = ErrorRed)
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DifficultyChip(label: String, count: Int, color: Color) {
    AssistChip(
        onClick = {},
        label = { Text("$label: $count") },
        colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.15f))
    )
}

@Composable
private fun InterviewCountdownCard(dateStr: String) {
    val daysLeft = remember(dateStr) {
        try {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dateStr))
        } catch (e: Exception) {
            null
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                val text = when {
                    daysLeft == null -> "Target interview date set"
                    daysLeft > 0 -> "$daysLeft day${if (daysLeft == 1L) "" else "s"} until your target interview"
                    daysLeft == 0L -> "Your target interview is today"
                    else -> "Your target interview date has passed"
                }
                Text(text = text, fontWeight = FontWeight.Bold)
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun InfoBanner(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(message)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun ProfileDetailsReadOnly(profile: ProfileDto?) {
    if (profile == null) return

    Text(text = "Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    DetailRow("Target companies", profile.targetCompanies.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Not set")
    DetailRow("Preferred language", profile.preferredLanguage.replaceFirstChar { it.uppercase() })
    DetailRow("GitHub", profile.githubHandle.takeIf { it.isNotBlank() } ?: "Not set")
    DetailRow("LinkedIn", profile.linkedinUrl.takeIf { it.isNotBlank() } ?: "Not set")
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileEditForm(
    draft: ProfileEditState,
    isSaving: Boolean,
    onDraftChange: ((ProfileEditState) -> ProfileEditState) -> Unit,
    onToggleCompany: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Text(text = "Edit profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = draft.name,
        onValueChange = { value -> onDraftChange { it.copy(name = value) } },
        label = { Text("Name") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = draft.bio,
        onValueChange = { value -> if (value.length <= 200) onDraftChange { it.copy(bio = value) } },
        label = { Text("Bio") },
        supportingText = { Text("${draft.bio.length}/200") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Target role", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TARGET_ROLES.forEach { role ->
            FilterChip(
                selected = draft.targetRole == role,
                onClick = { onDraftChange { it.copy(targetRole = if (it.targetRole == role) "" else role) } },
                label = { Text(role) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Experience level", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EXPERIENCE_LEVELS.forEach { level ->
            FilterChip(
                selected = draft.experienceLevel == level,
                onClick = { onDraftChange { it.copy(experienceLevel = if (it.experienceLevel == level) "" else level) } },
                label = { Text(level) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Target companies", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TARGET_COMPANIES.forEach { company ->
            FilterChip(
                selected = draft.targetCompanies.contains(company),
                onClick = { onToggleCompany(company) },
                label = { Text(company) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Preferred language", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val options = listOf(
            Triple("python", "Python", 0),
            Triple("java", "Java", 1),
            Triple("cpp", "C++", 2)
        )
        options.forEach { (value, label, index) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                selected = draft.preferredLanguage == value,
                onClick = { onDraftChange { it.copy(preferredLanguage = value) } }
            ) {
                Text(label)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = draft.githubHandle,
        onValueChange = { value -> onDraftChange { it.copy(githubHandle = value) } },
        label = { Text("GitHub username") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = draft.linkedinUrl,
        onValueChange = { value -> onDraftChange { it.copy(linkedinUrl = value) } },
        label = { Text("LinkedIn URL") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Target interview date", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { showDatePicker = true }) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(draft.interviewTargetDate ?: "Not set")
        }
        if (draft.interviewTargetDate != null) {
            TextButton(onClick = { onDraftChange { it.copy(interviewTargetDate = null) } }) {
                Text("Clear")
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !isSaving) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel")
        }
        Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = !isSaving) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.interviewTargetDate?.let {
                try {
                    LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDraftChange { it.copy(interviewTargetDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
                    }
                    showDatePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun initialsFor(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts[1].first()}".uppercase()
    } else {
        parts[0].take(2).uppercase()
    }
}

private fun memberSinceLabel(createdAt: String?): String? {
    if (createdAt.isNullOrBlank()) return null
    return try {
        val date = Instant.parse(createdAt).atZone(ZoneOffset.UTC).toLocalDate()
        "Member since ${date.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
    } catch (e: Exception) {
        null
    }
}