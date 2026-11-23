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

        item { AppearanceCard(ui, viewModel) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SettingRow(
                        title = "Auto-detect output device",
                        subtitle = "Load the right profile when you connect Bluetooth, USB DAC or wired audio",
                        checked = ui.autoApply,
                    ) { viewModel.setAutoApply(it) }
                    SettingRow(
                        title = "Auto gain protection",
                        subtitle = "Lowers preamp as you boost EQ or bass to reduce clipping",
                        checked = ui.autoGainProtection,
                    ) { viewModel.setAutoGainProtection(it) }
                    SettingRow(
                        title = "Keep EQ active in background",
                        subtitle = "Reapplies your sound when other apps change it, so it works across all players",
                        checked = ui.keepAlive,
                    ) { viewModel.setKeepAlive(it) }
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
                    val status = ui.engineStatus
                    Text(
                        text = status?.detail ?: "Checking audio engine…",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (status?.ready == true && status.hasControl) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        text = "${ui.bandCount} bands · ${ui.minLevel / 100} to ${ui.maxLevel / 100} dB · " +
                            "app sessions: ${status?.attachedSessions ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Bass boost: ${if (ui.bassBoostSupported) "supported" else "unsupported"} · " +
                            "Surround: ${if (ui.virtualizerSupported) "supported" else "unsupported"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (status?.hasControl == false) {
                        Text(
                            text = "Close any other equalizer app, then tap Reinitialize. Amla uses a higher " +
                                "priority so it can take control of the system equalizer.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = { viewModel.reinitializeEffects() }) {
                        Text("Reinitialize effects")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("YouTube / low volume", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "1. While the song plays, open the notification shade, expand the media " +
                            "notification and push its volume slider to maximum — YouTube has its own " +
                            "volume that can be set lower than the system volume.\n" +
                            "2. Press volume up while YouTube is playing and make sure the slider is at max.\n" +
                            "3. Turn off Battery Saver and any 'sound booster' inside YouTube.\n" +
                            "4. Use the Loudness boost slider on the Equalizer tab if EQ makes the song " +
                            "sound quieter.\n" +
                            "Amla attaches to the global output, so it works with any player that uses " +
                            "Android's normal audio output. Some apps use their own DSP and cannot be " +
                            "processed by any system equalizer.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (ui.isRealme) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Realme / Dirac sound", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Your phone has Realme's built-in Dirac sound effect. Android turns off " +
                                "third-party global EQ while a per-app effect is active. Open " +
                                "Settings → Sound & vibration → Real Sound / Sound effects and switch it " +
                                "OFF (choose 'Original'). Also turn off any equalizer or bass boost inside " +
                                "your music app, then tap Reinitialize above.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
