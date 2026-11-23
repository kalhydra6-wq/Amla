package com.amala.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amala.app.audio.NowPlayingRepository
import com.amala.app.audio.UsbDeviceLookup
import com.amala.app.model.AudioDevice
import com.amala.app.model.AudioSignal
import com.amala.app.model.UsbIdentity
import com.amala.app.viewmodel.MainViewModel

@Composable
fun SignalChainCard(ui: MainViewModel.UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val nowPlaying by NowPlayingRepository.state.collectAsStateWithLifecycle()
    val device = ui.selectedDevice ?: ui.activeDevice

    LaunchedEffect(Unit) { NowPlayingRepository.refresh(context) }
    LaunchedEffect(device?.id) { NowPlayingRepository.refresh(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { NowPlayingRepository.refresh(context) }

    val border = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = border, shape = RoundedCornerShape(20.dp))
            .padding(1.dp),
    ) {
        Card(
            shape = RoundedCornerShape(19.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Header(device, context)
                Spacer(Modifier.height(10.dp))
                NowPlayingBlock(nowPlaying, context)
                Spacer(Modifier.height(14.dp))
                ChainBlocks(device, ui, context)
                Spacer(Modifier.height(12.dp))
                Badges(ui, device, context)
                Spacer(Modifier.height(12.dp))
                CompactRow(device)
            }
        }
    }
}

@Composable
private fun Header(device: AudioDevice?, context: Context) {
    val usb: UsbIdentity? = remember(device) { UsbDeviceLookup.find(context, device) }
    Text(
        text = "LIVE AUDIO SIGNAL CHAIN",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = device?.displayName ?: "No output device",
        style = MaterialTheme.typography.titleMedium,
    )
    if (usb != null) {
        Text(
            text = listOfNotNull(
                usb.manufacturer,
                "VID ${AudioSignal.formatVendorId(usb.vendorId)}",
                "PID ${AudioSignal.formatVendorId(usb.productId)}",
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = device?.typeLabel ?: "Waiting for audio output",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NowPlayingBlock(
    info: com.amala.app.model.NowPlayingInfo,
    context: Context,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val bitmap = info.artwork as? Bitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = info.title ?: "No track info",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(info.artist, info.album).joinToString(" · ")
                    .ifBlank { AudioSignal.NA },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Source: ${info.sourceApp ?: AudioSignal.NA}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    if (!info.permissionGranted) {
        Text(
            text = "Allow notification access to show song, artist and album art. Amla only reads media session metadata — no notifications are stored or sent anywhere.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }) {
            Text("Open notification access")
        }
    }
}

@Composable
private fun ChainBlocks(device: AudioDevice?, ui: MainViewModel.UiState, context: Context) {
    val outputRate = outputRate(context, device)
    val bestEncoding = device?.encodings
        ?.filter { it == android.media.AudioFormat.ENCODING_PCM_FLOAT || it == android.media.AudioFormat.ENCODING_PCM_16BIT || it == android.media.AudioFormat.ENCODING_PCM_8BIT }
        ?.maxByOrNull { AudioSignal.bitDepth(it) ?: 0 }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("INPUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            SpecLine("Sample rate", AudioSignal.NA)
            SpecLine("Bit depth", AudioSignal.NA)
            SpecLine("Channels", AudioSignal.NA)
            SpecLine("Codec", AudioSignal.NA)
            SpecLine("Bitrate", AudioSignal.NA)
        }

        DspChip(active = ui.effectsEnabled && ui.engineStatus?.hasControl == true)

        Column(Modifier.weight(1f)) {
            Text("OUTPUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            SpecLine("Sample rate", AudioSignal.formatRate(outputRate))
            SpecLine("Bit depth", AudioSignal.formatDepth(bestEncoding))
            SpecLine("Channels", AudioSignal.formatChannels(device?.channelCounts?.maxOrNull()))
            SpecLine("Codec", AudioSignal.codecName(bestEncoding))
            SpecLine("Type", device?.category?.displayName ?: AudioSignal.NA)
        }
    }
}

@Composable
private fun DspChip(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "dsp")
    val shift by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "dsp-shift",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(86.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (active) "Amla DSP" else "DSP OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "→",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = shift.dp),
        )
    }
}

@Composable
private fun SpecLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Badges(ui: MainViewModel.UiState, device: AudioDevice?, context: Context) {
    val outputRate = outputRate(context, device)
    val bestEncoding = device?.encodings
        ?.filter { AudioSignal.bitDepth(it) != null }
        ?.maxByOrNull { AudioSignal.bitDepth(it) ?: 0 }
    val effects = buildList {
        add("EQ")
        if (ui.profile.bassBoost > 0) add("Bass")
        if (ui.profile.virtualizer > 0) add("Virtualizer")
        if (ui.profile.loudness > 0) add("Loudness")
        if (ui.profile.reverb > 0) add("Reverb")
        if (ui.profile.limiterEnabled) add("Limiter")
    }
    val badges = buildList {
        if (AudioSignal.isResampled(null, outputRate)) add("Resampled")
        if (AudioSignal.isHiRes(outputRate, bestEncoding)) add("Hi-Res")
        add(if (ui.effectsEnabled && ui.engineStatus?.hasControl == true) "Amla DSP ON" else "Amla DSP OFF")
        addAll(effects)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        badges.take(6).forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CompactRow(device: AudioDevice?) {
    val rate = outputRate(LocalContext.current, device)
    val encoding = device?.encodings?.filter { AudioSignal.bitDepth(it) != null }
        ?.maxByOrNull { AudioSignal.bitDepth(it) ?: 0 }
    val items = listOf(
        "CODEC" to AudioSignal.codecName(encoding),
        "RATE" to AudioSignal.formatRateCompact(rate),
        "DEPTH" to AudioSignal.formatDepth(encoding),
        "CH" to AudioSignal.formatChannels(device?.channelCounts?.maxOrNull()),
        "BITRATE" to AudioSignal.NA,
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        items.forEach { (label, value) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private fun outputRate(context: Context, device: AudioDevice?): Int? {
    val property = runCatching {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
    }.getOrNull()
    if (property != null && property > 0) return property
    return device?.sampleRates?.maxOrNull()?.takeIf { it > 0 }
}
