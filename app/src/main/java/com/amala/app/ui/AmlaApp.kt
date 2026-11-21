package com.amala.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amala.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmlaApp(viewModel: MainViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.EQUALIZER) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        ui.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            Modifier.padding(end = 8.dp),
                        )
                        Column {
                            Text(text = "Amla", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Smart audio equalizer",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { screen = Screen.SETTINGS }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Screen.entries.forEach { entry ->
                    val selected = entry == screen
                    val buttonModifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                    val label = @Composable {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    if (selected) {
                        Button(
                            onClick = { screen = entry },
                            modifier = buttonModifier,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) { label() }
                    } else {
                        OutlinedButton(
                            onClick = { screen = entry },
                            modifier = buttonModifier,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) { label() }
                    }
                }
            }

            when (screen) {
                Screen.EQUALIZER -> EqualizerScreen(ui, viewModel)
                Screen.PROFILES -> ProfilesScreen(ui, viewModel)
                Screen.SETTINGS -> SettingsScreen(ui, viewModel)
                Screen.ABOUT -> AboutScreen()
            }
        }
    }
}
