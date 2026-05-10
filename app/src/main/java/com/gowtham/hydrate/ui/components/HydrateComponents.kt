package com.gowtham.hydrate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.gowtham.hydrate.data.model.ReminderSlot

@Composable
fun ProgressRing(progress: Float, size: Dp, strokeWidth: Dp) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progress_ring")
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = bgColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx),
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun QuickAddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
}

@Composable
fun TimelineItem(slot: ReminderSlot, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                slot.skipped -> MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                slot.current -> MaterialTheme.colorScheme.surfaceVariant
                slot.upcoming -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            },
        ),
        border = BorderStroke(
            1.dp,
            when {
                slot.skipped -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                slot.current -> Color.White
                else -> MaterialTheme.colorScheme.outline
            },
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.timeLabel, style = MaterialTheme.typography.titleLarge)
                Text("${slot.amountMl} ml · ${slot.cumulativeMl} ml cumulative", color = MaterialTheme.colorScheme.secondary)
            }
            Text(
                text = when {
                    slot.skipped -> "Skipped"
                    slot.completed -> "✓"
                    slot.current -> "Current"
                    else -> "Upcoming"
                },
                color = if (slot.completed || slot.skipped) Color.White else MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
fun StreakBadge(text: String) {
    Box(
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
fun ReminderCountdown(label: String, countdown: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Next reminder", color = MaterialTheme.colorScheme.secondary)
        Text("$label in $countdown", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = { Text(message) },
    )
}
