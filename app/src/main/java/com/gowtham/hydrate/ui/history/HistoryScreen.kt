package com.gowtham.hydrate.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gowtham.hydrate.ui.HydrateUiState
import com.gowtham.hydrate.ui.components.StatCard
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(uiState: HydrateUiState, onBack: () -> Unit) {
    val stats = uiState.recentStats
    val statsByDate = stats.associateBy { LocalDate.parse(it.date) }
    val latestDate = statsByDate.keys.maxOrNull() ?: LocalDate.now()
    val weekly = (0 until 7).map { latestDate.minusDays((6 - it).toLong()) }.map { date ->
        val dayStats = statsByDate[date]
        DayBar(
            label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
            amountMl = dayStats?.totalMl ?: 0,
            hitGoal = dayStats?.goalCompleted == true,
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("History", style = MaterialTheme.typography.displaySmall)
        Text("Hydration analytics at a glance", color = MaterialTheme.colorScheme.secondary)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(title = "Streak", value = "${uiState.historySummary.currentStreak} days", modifier = Modifier.weight(1f))
            StatCard(title = "Longest", value = "${uiState.historySummary.longestStreak} days", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(title = "Best day", value = "${uiState.historySummary.bestDayMl} ml", modifier = Modifier.weight(1f))
            StatCard(title = "Weekly avg", value = formatLiters(uiState.historySummary.averageMl), modifier = Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().height(240.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
                WeeklyBarChart(days = weekly)
                Text(
                    text = "${uiState.historySummary.weeklyGoalHitDays}/7 days hit goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "You drink ${formatLiters(uiState.historySummary.averageMl)} on average this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun WeeklyBarChart(days: List<DayBar>) {
    val max = (days.maxOfOrNull { it.amountMl } ?: 1).coerceAtLeast(1)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        days.forEach { day ->
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.size(width = 18.dp, height = 140.dp)) {
                    val barHeight = size.height * (day.amountMl.toFloat() / max.toFloat())
                    drawRoundRect(
                        color = if (day.hitGoal) Color.White else Color(0xFF6E6E6E),
                        topLeft = Offset(0f, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    )
                }
                Text(day.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private data class DayBar(
    val label: String,
    val amountMl: Int,
    val hitGoal: Boolean,
)

private fun formatLiters(amountMl: Int): String = String.format(Locale.US, "%.1fL", amountMl / 1000f)
