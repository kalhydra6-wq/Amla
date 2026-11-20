package com.amala.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amala.app.viewmodel.MainViewModel

@Composable
fun ProfilesScreen(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Saved profiles", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Amla remembers your EQ for every device it detects.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (ui.storedProfiles.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No custom profiles yet. Adjust the equalizer for your device and tap Save.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        items(ui.storedProfiles, key = { it.first }) { entry ->
            val (deviceId, profile) = entry
            val deviceName = ui.devices.firstOrNull { it.id == deviceId }?.displayName
                ?: deviceId.substringAfter(':').replace('_', ' ')
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(deviceName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${profile.preset.label} · ${profile.bands.size} bands · bass ${profile.bassBoost / 10}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.applyStoredProfile(deviceId) },
                            modifier = Modifier.weight(1f),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Text("Apply")
                        }
                        OutlinedButton(
                            onClick = { viewModel.deleteProfile(deviceId) },
                            modifier = Modifier.weight(1f),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
