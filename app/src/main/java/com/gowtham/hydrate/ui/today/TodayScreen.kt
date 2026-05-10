package com.gowtham.hydrate.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gowtham.hydrate.data.model.TodaySummary
import com.gowtham.hydrate.ui.HydrateUiState
import com.gowtham.hydrate.ui.components.ProgressRing
import com.gowtham.hydrate.ui.components.QuickAddButton
import com.gowtham.hydrate.ui.components.ReminderCountdown
import com.gowtham.hydrate.ui.components.StatCard
import com.gowtham.hydrate.ui.components.StreakBadge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    uiState: HydrateUiState,
    onQuickAdd: (Int) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onUndoLastLog: () -> Unit,
    showTabTips: Boolean = false,
    shouldCelebrateGoal: Boolean = false,
    onDismissTabTips: () -> Unit = {},
    onCelebrationDisplayed: () -> Unit = {},
) {
    var showUndo by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(shouldCelebrateGoal) }

    LaunchedEffect(shouldCelebrateGoal) {
        if (shouldCelebrateGoal) {
            showCelebration = true
            delay(4_000)
            showCelebration = false
            onCelebrationDisplayed()
        }
    }

    LaunchedEffect(showUndo) {
        if (showUndo) {
            delay(5_000)
            showUndo = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderSection(summary = uiState.todaySummary, streakDays = uiState.historySummary.currentStreak)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressRing(progress = uiState.todaySummary.percent / 100f, size = 220.dp, strokeWidth = 18.dp)
                    Text("${uiState.todaySummary.totalMl} / ${uiState.todaySummary.goalMl} ml", style = MaterialTheme.typography.headlineMedium)
                    Text("${uiState.todaySummary.percent}% complete", color = MaterialTheme.colorScheme.secondary)
                    Text(uiState.todaySummary.message, style = MaterialTheme.typography.titleLarge)
                    ReminderCountdown(
                        label = uiState.todaySummary.nextReminderLabel,
                        countdown = uiState.todaySummary.nextReminderCountdown,
                    )
                    uiState.todaySummary.carryOverSuggestion?.let { carryOver ->
                        Text(carryOver, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    uiState.todaySummary.weatherSuggestion?.let { weather ->
                        Text(weather, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAddButton(label = "+1 Cup", onClick = {
                    onQuickAdd(uiState.preferences.cupSizeMl)
                    showUndo = true
                }, modifier = Modifier.weight(1f))
                QuickAddButton(label = "+250 ml", onClick = {
                    onQuickAdd(250)
                    showUndo = true
                }, modifier = Modifier.weight(1f))
                QuickAddButton(label = "+500 ml", onClick = {
                    onQuickAdd(500)
                    showUndo = true
                }, modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(title = "Streak", value = "${uiState.historySummary.currentStreak}d", modifier = Modifier.weight(1f))
                StatCard(title = "Average", value = "${uiState.historySummary.averageMl} ml", modifier = Modifier.weight(1f))
                StatCard(title = "Best", value = "${uiState.historySummary.bestDayMl} ml", modifier = Modifier.weight(1f))
            }

            StreakBadge(text = "${uiState.historySummary.currentStreak} day streak")

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Today’s log", style = MaterialTheme.typography.titleLarge)
                    uiState.todayLogs.take(6).forEach { log ->
                        LogRow(timestampMillis = log.timestampMillis, amountMl = log.amountMl)
                    }
                    if (uiState.todayLogs.isEmpty()) {
                        Text("No water logged yet.", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenSchedule, modifier = Modifier.weight(1f)) { Text("Schedule") }
                OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) { Text("History") }
                Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Settings") }
            }
        }

        if (showUndo) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Logged water", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        onUndoLastLog()
                        showUndo = false
                    }) {
                        Text("Undo")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(summary: TodaySummary, streakDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Today", style = MaterialTheme.typography.displaySmall)
            Text("Wake up, drink well, sleep light.", color = MaterialTheme.colorScheme.secondary)
        }
        StreakBadge(text = "$streakDays days")
    }
}

@Composable
private fun LogRow(timestampMillis: Long, amountMl: Int) {
    val formatter = rememberTimeFormatter()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(formatter))
        Text("+$amountMl ml", color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun rememberTimeFormatter(): DateTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
