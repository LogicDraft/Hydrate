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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

@Composable
fun HistoryScreen(uiState: HydrateUiState, onBack: () -> Unit) {
    val stats = uiState.recentStats
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("History", style = MaterialTheme.typography.displaySmall)
        Text("Hydration analytics at a glance", color = MaterialTheme.colorScheme.secondary)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(title = "Current streak", value = "${uiState.historySummary.currentStreak}d", modifier = Modifier.weight(1f))
            StatCard(title = "Longest", value = "${uiState.historySummary.longestStreak}d", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(title = "Best day", value = "${uiState.historySummary.bestDayMl} ml", modifier = Modifier.weight(1f))
            StatCard(title = "Average", value = "${uiState.historySummary.averageMl} ml", modifier = Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().height(240.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
                WeeklyBarChart(values = stats.take(7).map { it.totalMl })
            }
        }

        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun WeeklyBarChart(values: List<Int>) {
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        values.reversed().forEach { value ->
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.size(width = 18.dp, height = 140.dp)) {
                    val barHeight = size.height * (value.toFloat() / max.toFloat())
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    )
                }
                Text("${value}ml", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
