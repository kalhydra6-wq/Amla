package com.amala.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amala.app.viewmodel.MainViewModel

@Composable
fun SettingsScreen(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SettingRow(
                        title = "Auto-detect output device",
                        subtitle = "Load the right profile when you connect Bluetooth, USB DAC or wired audio",
                        checked = ui.autoApply,
                    ) { viewModel.setAutoApply(it) }
                    SettingRow(
                        title = "Enable audio effects",
                        subtitle = "Bypass or engage the equalizer instantly",
                        checked = ui.effectsEnabled,
                    ) { viewModel.setEffectsEnabled(it) }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Audio engine", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (ui.engineReady) {
                            "Active · ${ui.bandCount} bands · ${ui.minLevel / 100} to ${ui.maxLevel / 100} dB"
                        } else {
                            "Unavailable on this device"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Bass boost: ${if (ui.bassBoostSupported) "supported" else "unsupported"} · " +
                            "Surround: ${if (ui.virtualizerSupported) "supported" else "unsupported"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { viewModel.reinitializeEffects() }) {
                        Text("Reinitialize effects")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Detected devices", style = MaterialTheme.typography.titleMedium)
                    if (ui.devices.isEmpty()) {
                        Text(
                            text = "No output devices found. Connect headphones or a DAC and tap Rescan.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ui.devices.forEach { device ->
                            Text(
                                text = "• ${device.displayName} — ${device.typeLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Amla attaches effects to Android's global output mix, so the EQ applies to music, " +
                        "video and games. Some manufacturers restrict global effects; if the sound does not " +
                        "change, try reinitializing or check the device's audio settings.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
