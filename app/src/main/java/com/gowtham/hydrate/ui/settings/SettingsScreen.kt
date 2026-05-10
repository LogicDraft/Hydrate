package com.gowtham.hydrate.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gowtham.hydrate.data.model.CupSize
import com.gowtham.hydrate.data.model.ReminderAlertMode
import com.gowtham.hydrate.data.model.UserPreferences
import com.gowtham.hydrate.ui.HydrateUiState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    uiState: HydrateUiState,
    onSave: (UserPreferences) -> Unit,
    onResetToday: () -> Unit,
    onEraseData: () -> Unit,
    onBack: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var wakeTime by remember { mutableStateOf(uiState.preferences.wakeTime) }
    var sleepTime by remember { mutableStateOf(uiState.preferences.sleepTime) }
    var dailyGoal by remember { mutableIntStateOf(uiState.preferences.dailyGoalMl) }
    var cupSize by remember { mutableIntStateOf(uiState.preferences.cupSizeMl) }
    var notificationsEnabled by remember { mutableStateOf(uiState.preferences.notificationsEnabled) }
    var snoozeMinutes by remember { mutableIntStateOf(uiState.preferences.snoozeMinutes) }
    var alertMode by remember { mutableStateOf(uiState.preferences.reminderAlertMode) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.displaySmall)
        Text("Tune your hydration ritual", color = MaterialTheme.colorScheme.secondary)

        PreferenceCard(title = "Wake / sleep") {
            Text("Wake ${wakeTime.format(timeFormatter)}")
            Text("Sleep ${sleepTime.format(timeFormatter)}")
        }

        PreferenceCard(title = "Goal") {
            OutlinedTextField(value = dailyGoal.toString(), onValueChange = { dailyGoal = it.filter(Char::isDigit).toIntOrNull() ?: dailyGoal }, label = { Text("Daily goal (ml)") }, modifier = Modifier.fillMaxWidth())
        }

        PreferenceCard(title = "Cup size") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CupSize.entries.forEach { option ->
                    FilterChip(selected = cupSize == option.milliliters || (option == CupSize.CUSTOM && cupSize == 0), onClick = { cupSize = option.milliliters }, label = { Text(option.label) })
                }
            }
        }

        PreferenceCard(title = "Notifications") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(onClick = { notificationsEnabled = !notificationsEnabled }, label = { Text(if (notificationsEnabled) "On" else "Off") })
                AssistChip(onClick = { snoozeMinutes = 60 }, label = { Text("Snooze $snoozeMinutes min") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = alertMode == ReminderAlertMode.GENTLE_SOUND, onClick = { alertMode = ReminderAlertMode.GENTLE_SOUND }, label = { Text("Gentle Sound") })
                FilterChip(selected = alertMode == ReminderAlertMode.PHONE_RINGTONE, onClick = { alertMode = ReminderAlertMode.PHONE_RINGTONE }, label = { Text("Phone Ringtone") })
                FilterChip(selected = alertMode == ReminderAlertMode.VIBRATION_ONLY, onClick = { alertMode = ReminderAlertMode.VIBRATION_ONLY }, label = { Text("Vibration Only") })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onResetToday, modifier = Modifier.weight(1f)) { Text("Reset today") }
            Button(onClick = onEraseData, modifier = Modifier.weight(1f)) { Text("Erase data") }
        }

        Button(
            onClick = {
                onSave(
                    UserPreferences(
                        wakeTime = wakeTime,
                        sleepTime = sleepTime,
                        dailyGoalMl = dailyGoal,
                        cupSizeMl = if (cupSize == 0) 300 else cupSize,
                        notificationsEnabled = notificationsEnabled,
                        reminderAlertMode = alertMode,
                        snoozeMinutes = snoozeMinutes,
                        onboarded = true,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun PreferenceCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
