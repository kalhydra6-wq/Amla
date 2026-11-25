package com.amala.app.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amala.app.R
import com.amala.app.data.AmlaPreferences
import com.amala.app.model.AccentOption
import com.amala.app.model.ThemeMode
import com.amala.app.viewmodel.MainViewModel

private val BUILT_IN = listOf(
    AccentOption("Amla Green", AmlaPreferences.AMLA_GREEN),
    AccentOption("Amla Purple", AmlaPreferences.AMLA_PURPLE),
)

private val ACCENTS = listOf(
    AccentOption("Teal", 0xFF007C70.toInt()),
    AccentOption("Green", 0xFF2E7D32.toInt()),
    AccentOption("Blue", 0xFF2957A8.toInt()),
    AccentOption("Indigo", 0xFF3F51B5.toInt()),
    AccentOption("Purple", 0xFF6D4BC4.toInt()),
    AccentOption("Pink", 0xFFC2185B.toInt()),
    AccentOption("Red", 0xFFB3261E.toInt()),
    AccentOption("Orange", 0xFFE65100.toInt()),
    AccentOption("Amber", 0xFFFF8F00.toInt()),
    AccentOption("Lime", 0xFF558B2F.toInt()),
    AccentOption("Cyan", 0xFF00838F.toInt()),
    AccentOption("Slate", 0xFF455A64.toInt()),
)

@Composable
fun AppearanceCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    var showResetDialog by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            Text("Theme mode", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ThemeMode.entries) { mode ->
                    FilterChip(
                        selected = ui.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.built_in_themes), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BUILT_IN) { theme ->
                    val active = ui.accentColor == theme.argb
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.setAccentColor(theme.argb) }
                            .background(Color(theme.argb))
                            .then(
                                if (active) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Accent colour", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ACCENTS) { option ->
                    val selected = ui.accentColor == option.argb
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(option.argb))
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            CustomColorPicker(
                current = ui.accentColor,
                onColor = { viewModel.setAccentColor(it) },
            )

            Spacer(Modifier.height(14.dp))
            ThemePreview(ui.accentColor)

            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reset_theme))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_theme)) },
            text = { Text(stringResource(R.string.reset_theme_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetTheme()
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CustomColorPicker(current: Int, onColor: (Int) -> Unit) {
    var hue by remember { mutableFloatStateOf(180f) }
    var saturation by remember { mutableFloatStateOf(0.75f) }
    var value by remember { mutableFloatStateOf(0.6f) }
    var hex by remember { mutableStateOf(hexOf(current)) }

    LaunchedEffect(current) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(current, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hex = hexOf(current)
    }

    fun applyHsv() {
        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        onColor(argb)
        hex = hexOf(argb)
    }

    Text("Custom colour", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(4.dp))
    PickerSlider("Hue", hue, 0f..360f) { hue = it; applyHsv() }
    PickerSlider("Saturation", saturation, 0f..1f) { saturation = it; applyHsv() }
    PickerSlider("Brightness", value, 0f..1f) { value = it; applyHsv() }

    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = hex,
        onValueChange = { input ->
            hex = input
            parseHex(input)?.let(onColor)
        },
        label = { Text("HEX") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun PickerSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.35f))
        Slider(value = value, valueRange = range, onValueChange = onChange, modifier = Modifier.weight(0.65f))
    }
}

private fun hexOf(argb: Int): String =
    String.format("#%06X", 0xFFFFFF and argb)

private fun parseHex(input: String): Int? {
    val raw = input.trim().removePrefix("#")
    return runCatching {
        when (raw.length) {
            6 -> (0xFF000000L or raw.toLong(16)).toInt()
            8 -> raw.toLong(16).toInt()
            else -> null
        }
    }.getOrNull()
}

@Composable
private fun ThemePreview(accentArgb: Int) {
    var checked by remember { mutableStateOf(true) }
    var slider by remember { mutableFloatStateOf(0.65f) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Live preview", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {}) { Text("Button") }
                Spacer(Modifier.height(0.dp))
                FilterChip(selected = true, onClick = {}, label = { Text("Chip") })
            }
            Spacer(Modifier.height(6.dp))
            Slider(value = slider, valueRange = 0f..1f, onValueChange = { slider = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Switch", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = { checked = it })
            }
            Text(
                text = "Accent #${hexOf(accentArgb)} applies to buttons, sliders, chips, switches and icons.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
