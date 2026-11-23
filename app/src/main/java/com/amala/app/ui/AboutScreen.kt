package com.amala.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.amala.app.viewmodel.MainViewModel

@Composable
fun AboutScreen(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Amla", style = MaterialTheme.typography.titleLarge)
                    Text("Version 1.0.0", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (ui.crashReport != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Last crash report", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = ui.crashReport.orEmpty(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        )
                        OutlinedButton(onClick = { viewModel.clearCrashReport() }) {
                            Text("Clear report")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("What it does", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Amla detects the active audio output — Bluetooth/LE Audio headphones, " +
                            "USB DACs, wired earphones or the built-in speaker — and automatically " +
                            "applies the equalizer profile you saved for it. Band frequencies come " +
                            "directly from the device's audio stack, so the sliders match real hardware.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Source code", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "github.com/kalhydra6-wq/Amla",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Built with Kotlin, Jetpack Compose and Android audio effects.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
