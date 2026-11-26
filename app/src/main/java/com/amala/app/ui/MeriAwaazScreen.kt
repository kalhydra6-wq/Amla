package com.amala.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amala.app.R
import com.amala.app.data.SoundEventSummary
import com.amala.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeriAwaazScreen(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    val events by viewModel.soundEvents.collectAsStateWithLifecycle()
    var showDetails by remember { mutableStateOf(false) }
    var enableDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.awaaz_title), style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.awaaz_intro),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.awaaz_learn), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.awaaz_learn_info),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = ui.learnHabits,
                            onCheckedChange = { checked ->
                                if (checked) enableDialog = true
                                else viewModel.setLearnHabits(false)
                            },
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.awaaz_retention), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 90, 365).forEach { days ->
                            FilterChip(
                                selected = ui.learnRetentionDays == days,
                                onClick = { viewModel.setLearnRetentionDays(days) },
                                label = { Text(stringResource(R.string.awaaz_days, days)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showDetails = !showDetails },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (showDetails) stringResource(R.string.awaaz_hide)
                                else stringResource(R.string.awaaz_see),
                            )
                        }
                        OutlinedButton(
                            onClick = { deleteDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.awaaz_delete))
                        }
                    }
                }
            }
        }

        if (showDetails) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (events.isEmpty()) {
                            Text(
                                stringResource(R.string.awaaz_no_events),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                            events.take(30).forEach { event ->
                                val what = event.effectName
                                    ?: stringResource(R.string.awaaz_event_band, event.bandIndex ?: 0)
                                Text(
                                    text = stringResource(
                                        R.string.awaaz_event,
                                        formatter.format(Date(event.timestamp)),
                                        event.deviceType,
                                        what,
                                        String.format(Locale.US, "%.1f", event.newValue),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.awaaz_counts), style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Device: ${SoundEventSummary.countByDevice(events)}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                "App: ${SoundEventSummary.countByApp(events)}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                "Hour: ${SoundEventSummary.countByHour(events)}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }

    if (enableDialog) {
        AlertDialog(
            onDismissRequest = { enableDialog = false },
            title = { Text(stringResource(R.string.awaaz_enable_title)) },
            text = { Text(stringResource(R.string.awaaz_enable_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setLearnHabits(true)
                    enableDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { enableDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text(stringResource(R.string.awaaz_delete)) },
            text = { Text(stringResource(R.string.awaaz_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllEvents()
                    deleteDialog = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
