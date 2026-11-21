package com.amala.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amala.app.model.DeviceCategory
import com.amala.app.model.Preset
import com.amala.app.model.ProfileMath
import com.amala.app.model.ReverbPreset
import com.amala.app.viewmodel.MainViewModel

@Composable
fun EqualizerScreen(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { DeviceCard(ui, viewModel) }
        item { MasterCard(ui, viewModel) }
        item { PresetCard(ui, viewModel) }
        item { BandsCard(ui, viewModel) }
        item { ExtrasCard(ui, viewModel) }
        item { ActionsRow(viewModel) }
    }
}

@Composable
private fun DeviceCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "OUTPUT DEVICE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon(ui.activeDevice?.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = ui.activeDevice?.displayName ?: "No output device detected",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = ui.activeDevice?.let { "${it.category.displayName} · ${it.typeLabel}" }
                            ?: "Connect a device to begin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (ui.autoApply) {
                    "Auto-detect on · your profile is applied when the output changes"
                } else {
                    "Auto-detect off · showing the last selected device"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (ui.devices.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Detected outputs", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.devices, key = { it.id }) { device ->
                        val selected = ui.selectedDevice?.id == device.id
                        val label = device.displayName
                        if (selected) {
                            Button(onClick = { viewModel.selectDevice(device) }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectDevice(device) }) { Text(label) }
                        }
                    }
                }
            }

            if (ui.activeDevice != null && ui.selectedDevice?.id != ui.activeDevice?.id) {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.selectActiveDevice() },
                ) {
                    Text("Back to active device")
                }
            }
        }
    }
}

@Composable
private fun MasterCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Equalizer", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Global effect for all audio apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ui.effectsEnabled,
                    onCheckedChange = { viewModel.setEffectsEnabled(it) },
                )
            }
            val status = ui.engineStatus
            if (status != null && (!status.ready || !status.hasControl)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = status.detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PresetCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Sound preset", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Preset.entries, key = { it.name }) { preset ->
                    if (ui.profile.preset == preset && !ui.profile.custom) {
                        Button(onClick = { viewModel.setPreset(preset) }) { Text(preset.label) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setPreset(preset) }) { Text(preset.label) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (ui.profile.custom) {
                    "Custom curve · saved for ${ui.selectedDevice?.displayName ?: "this device"}"
                } else {
                    "${ui.profile.preset.label} preset · tuned for ${ui.selectedDevice?.category?.displayName ?: "this device"}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BandsCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Frequency bands (${ui.bandCount})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            ui.profile.bands.forEachIndexed { index, level ->
                val frequency = ui.frequencies.getOrNull(index) ?: (index + 1) * 1000
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ProfileMath.formatFrequency(frequency),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(62.dp),
                    )
                    Slider(
                        value = level.coerceIn(ui.minLevel, ui.maxLevel).toFloat(),
                        valueRange = ui.minLevel.toFloat()..ui.maxLevel.toFloat(),
                        onValueChange = { value -> viewModel.setBand(index, value.toInt()) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = ProfileMath.formatLevel(level),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(68.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtrasCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Sound enhancement", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            EffectSlider(
                title = "Bass boost",
                value = ui.profile.bassBoost,
                enabled = ui.bassBoostSupported,
            ) { viewModel.setBassBoost(it) }
            EffectSlider(
                title = "Surround / virtualizer",
                value = ui.profile.virtualizer,
                enabled = ui.virtualizerSupported,
            ) { viewModel.setVirtualizer(it) }
            EffectSlider(
                title = "Loudness boost",
                value = ui.profile.loudness,
                enabled = ui.loudnessSupported,
            ) { viewModel.setLoudness(it) }
            Spacer(Modifier.height(4.dp))
            Text("Reverb / room sound", style = MaterialTheme.typography.bodyMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ReverbPreset.entries) { room ->
                    val index = room.ordinal
                    if (ui.profile.reverb == index) {
                        Button(
                            onClick = { viewModel.setReverb(index) },
                            enabled = ui.reverbSupported,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) { Text(room.label) }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.setReverb(index) },
                            enabled = ui.reverbSupported,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) { Text(room.label) }
                    }
                }
            }
            if (!ui.bassBoostSupported || !ui.virtualizerSupported ||
                !ui.loudnessSupported || !ui.reverbSupported
            ) {
                Text(
                    text = "Some effects are not supported by this device's audio stack.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EffectSlider(
    title: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${value / 10}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.toFloat(),
            valueRange = 0f..1_000f,
            onValueChange = { onValueChange(it.toInt()) },
            enabled = enabled,
        )
    }
}

@Composable
private fun ActionsRow(viewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { viewModel.saveCustom() },
            modifier = Modifier.weight(1f),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Text("Save")
        }
        OutlinedButton(
            onClick = { viewModel.resetProfile() },
            modifier = Modifier.weight(1f),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Text("Reset")
        }
        OutlinedButton(
            onClick = { viewModel.refreshDevices() },
            modifier = Modifier.weight(1f),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Text("Rescan")
        }
    }
}

private fun categoryIcon(category: DeviceCategory?): ImageVector = when (category) {
    DeviceCategory.BLUETOOTH -> Icons.Filled.Bluetooth
    DeviceCategory.WIRED -> Icons.Filled.Headphones
    DeviceCategory.SPEAKER -> Icons.Filled.VolumeUp
    else -> Icons.Filled.MusicNote
}
