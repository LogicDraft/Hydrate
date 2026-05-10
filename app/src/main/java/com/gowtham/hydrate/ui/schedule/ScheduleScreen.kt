package com.gowtham.hydrate.ui.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Schedule", style = MaterialTheme.typography.headlineLarge)
        Text("Your reminders will appear here.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Back") }
    }
}
