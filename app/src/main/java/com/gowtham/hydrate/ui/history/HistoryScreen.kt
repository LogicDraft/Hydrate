package com.gowtham.hydrate.ui.history

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
fun HistoryScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("History", style = MaterialTheme.typography.headlineLarge)
        Text("Weekly chart and streak stats will appear here.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Back") }
    }
}
