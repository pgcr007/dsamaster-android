package com.dsamaster.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsamaster.app.BuildConfig
import com.dsamaster.app.DsaMasterApplication
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.ui.viewmodel.SettingsViewModel
import com.dsamaster.app.ui.viewmodel.SettingsViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as DsaMasterApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Appearance",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                Triple(UserPreferences.THEME_MODE_SYSTEM, "System", 0),
                Triple(UserPreferences.THEME_MODE_LIGHT, "Light", 1),
                Triple(UserPreferences.THEME_MODE_DARK, "Dark", 2)
            )
            options.forEach { (value, label, index) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    selected = uiState.themeMode == value,
                    onClick = { viewModel.setThemeMode(value) }
                ) {
                    Text(label)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Notifications",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        SettingsSwitchRow(
            title = "Daily reminders",
            subtitle = "Get nudged if you haven't hit today's goal",
            checked = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsClickableRow(
            title = "Reminder time",
            subtitle = formatTime(uiState.reminderHour, uiState.reminderMinute),
            enabled = uiState.notificationsEnabled,
            onClick = { showTimePicker = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsSwitchRow(
            title = "Weekly summary",
            subtitle = "A recap of problems solved and reviews done, every Sunday",
            checked = uiState.weeklySummaryEnabled,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.setWeeklySummaryEnabled(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Daily goal",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        SettingsClickableRow(
            title = "Problems per day",
            subtitle = "${uiState.dailyGoal} problem${if (uiState.dailyGoal == 1) "" else "s"}",
            enabled = false,
            onClick = { /* stepper below handles this instead of a click target */ }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                enabled = uiState.dailyGoal > 1,
                onClick = { viewModel.setDailyGoal(uiState.dailyGoal - 1) }
            ) {
                Text("−")
            }
            TextButton(
                onClick = { viewModel.setDailyGoal(uiState.dailyGoal + 1) }
            ) {
                Text("+")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Backup & Restore",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri -> uri?.let { viewModel.exportBackup(it) } }

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> uri?.let { viewModel.restoreBackup(it) } }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = {
                exportLauncher.launch("dsamaster_backup_${LocalDate.now()}.json")
            }) {
                Text("Export backup")
            }
            TextButton(onClick = {
                importLauncher.launch(arrayOf("application/json"))
            }) {
                Text("Restore from file")
            }
        }

        val backupMessage by viewModel.backupMessage.collectAsState()
        backupMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = { viewModel.clearBackupMessage() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        if (BuildConfig.DEBUG) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Debug: test notifications",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { viewModel.testRunDailyReminder() }) {
                    Text("Daily")
                }
                TextButton(onClick = { viewModel.testRunStreakRisk() }) {
                    Text("Streak risk")
                }
                TextButton(onClick = { viewModel.testRunWeeklySummary() }) {
                    Text("Weekly")
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(text = title)
            Text(text = subtitle)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title)
            Text(text = subtitle)
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val time = LocalTime.of(hour, minute)
    return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}