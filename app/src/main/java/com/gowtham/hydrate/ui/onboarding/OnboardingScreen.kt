package com.gowtham.hydrate.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gowtham.hydrate.data.model.CupSize
import com.gowtham.hydrate.data.model.UserPreferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialPreferences: UserPreferences,
    onSave: (UserPreferences) -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var wakeTime by remember { mutableStateOf(initialPreferences.wakeTime) }
    var sleepTime by remember { mutableStateOf(initialPreferences.sleepTime) }
    var dailyGoal by remember { mutableIntStateOf(initialPreferences.dailyGoalMl) }
    var cupSize by remember { mutableIntStateOf(initialPreferences.cupSizeMl) }
    var customCup by remember { mutableIntStateOf(if (initialPreferences.cupSizeMl == 0) 300 else initialPreferences.cupSizeMl) }

    val reminderCount = kotlin.math.ceil(dailyGoal / maxOf(1, effectiveCupSize(cupSize, customCup)).toDouble()).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(text = "Hydrate", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "A monochrome hydration ritual tuned to your day.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )

        MonochromeSetupCard(title = "Wake time") {
            TimePickerRow(label = wakeTime.format(timeFormatter), time = wakeTime, onTimeSelected = { wakeTime = it })
        }
        MonochromeSetupCard(title = "Sleep time") {
            TimePickerRow(label = sleepTime.format(timeFormatter), time = sleepTime, onTimeSelected = { sleepTime = it })
        }

        MonochromeSetupCard(title = "Daily goal") {
            OutlinedTextField(
                value = dailyGoal.toString(),
                onValueChange = { dailyGoal = it.filter(Char::isDigit).toIntOrNull() ?: dailyGoal },
                label = { Text("Goal in ml") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        MonochromeSetupCard(title = "Cup size") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CupSize.entries.forEach { option ->
                    FilterChip(
                        selected = cupSize == option.milliliters || (option == CupSize.CUSTOM && cupSize == 0),
                        onClick = { cupSize = option.milliliters },
                        label = { Text(option.label) },
                    )
                }
            }
            if (cupSize == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customCup.toString(),
                    onValueChange = { customCup = it.filter(Char::isDigit).toIntOrNull() ?: customCup },
                    label = { Text("Custom cup ml") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        MonochromeSetupCard(title = "Preview") {
            Text("$reminderCount reminders per day", style = MaterialTheme.typography.headlineSmall)
            Text("Wake ${wakeTime.format(timeFormatter)} · Sleep ${sleepTime.format(timeFormatter)}")
            Text("Goal $dailyGoal ml · Cup ${effectiveCupSize(cupSize, customCup)} ml")
        }

        Button(
            onClick = {
                onSave(
                    UserPreferences(
                        wakeTime = wakeTime,
                        sleepTime = sleepTime,
                        dailyGoalMl = dailyGoal,
                        cupSizeMl = effectiveCupSize(cupSize, customCup),
                        notificationsEnabled = true,
                        snoozeMinutes = 60,
                        onboarded = true,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save settings")
        }
    }
}

@Composable
private fun MonochromeSetupCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerRow(
    label: String,
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)

    androidx.compose.material3.OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }

    if (showPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                Button(onClick = {
                    onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
                    showPicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                AssistChip(onClick = { showPicker = false }, label = { Text("Cancel") })
            },
            title = { Text("Pick time") },
            text = { TimePicker(state = pickerState) },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

private fun effectiveCupSize(selected: Int, custom: Int): Int = if (selected == 0) custom.coerceAtLeast(1) else selected
