package com.amala.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amala.app.ui.theme.AmlaTheme
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

    AmlaTheme(themeMode = ui.themeMode, accentColor = ui.accentColor) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                                        style = MaterialTheme.typography.labelMedium,
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
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Screen.entries.forEach { entry ->
                            val selected = entry == screen
                            val modifier = Modifier
                                .width(104.dp)
                                .height(42.dp)
                            if (selected) {
                                Button(
                                    onClick = { screen = entry },
                                    modifier = modifier,
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(entry.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { screen = entry },
                                    modifier = modifier,
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(entry.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(140))
                        },
                        label = "Amla screen",
                    ) { target ->
                        when (target) {
                            Screen.EQUALIZER -> EqualizerScreen(ui, viewModel)
                            Screen.PROFILES -> ProfilesScreen(ui, viewModel)
                            Screen.MERI_AWAZ -> MeriAwaazScreen(ui, viewModel)
                            Screen.SETTINGS -> SettingsScreen(ui, viewModel)
                            Screen.ABOUT -> AboutScreen(ui, viewModel)
                        }
                    }
                }
            }
        }
    }
}
