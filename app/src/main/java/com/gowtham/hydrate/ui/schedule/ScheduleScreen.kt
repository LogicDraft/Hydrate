package com.gowtham.hydrate.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.gowtham.hydrate.ui.HydrateUiState
import com.gowtham.hydrate.ui.components.TimelineItem
import com.gowtham.hydrate.data.model.ReminderSlot

@Composable
fun ScheduleScreen(
    uiState: HydrateUiState,
    onSnooze: (Long, Int) -> Unit,
    onSkip: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var selectedSlot by remember { mutableStateOf<ReminderSlot?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Schedule", style = MaterialTheme.typography.displaySmall)
        Text("Generated reminder timeline", color = MaterialTheme.colorScheme.secondary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        ) {
            LazyColumn(modifier = Modifier.padding(18.dp)) {
                items(uiState.schedule) { slot ->
                    TimelineItem(
                        slot = slot,
                        onClick = {
                            if (!slot.completed && !slot.skipped) {
                                selectedSlot = slot
                            }
                        },
                    )
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }

    selectedSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { selectedSlot = null },
            title = { Text(slot.timeLabel) },
            text = { Text("Choose what to do with this reminder slot.") },
            confirmButton = {
                Button(onClick = {
                    onSnooze(slot.timestampMillis, slot.amountMl)
                    selectedSlot = null
                }) {
                    Text("Snooze 15 min")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    onSkip(slot.timestampMillis)
                    selectedSlot = null
                }) {
                    Text("Skip")
                }
            },
        )
    }
}
