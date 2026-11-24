package com.amala.app.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Slowly raises media volume after an output device change to protect the ears. */
object SoftStartController {

    private var fadeJob: Job? = null
    private var lastFadeAt = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun onOutputDeviceChanged(context: Context, enabled: Boolean, seconds: Int) {
        if (!enabled) return
        // Two observers (service + UI) can see the same change; ignore duplicates.
        val now = SystemClock.elapsedRealtime()
        if (now - lastFadeAt < DEBOUNCE_MS) return
        lastFadeAt = now

        val power = runCatching {
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        }.getOrNull()
        if (power?.isInteractive == false) return

        val manager = runCatching {
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }.getOrNull() ?: return

        fadeJob?.cancel()
        fadeJob = scope.launch {
            val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { manager.getStreamMinVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
            } else {
                0
            }
            val target = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (target <= min) return@launch

            val start = (min + (target - min) * 0.15f).toInt().coerceIn(min, max)
            val duration = seconds.coerceIn(1, 5) * 1_000L
            val steps = 20
            runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, start, 0) }
            repeat(steps) { index ->
                delay(duration / steps)
                val value = start + ((target - start) * (index + 1) / steps)
                runCatching {
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, value.coerceIn(min, max), 0)
                }
            }
        }
    }

    fun cancel() {
        fadeJob?.cancel()
        fadeJob = null
    }

    private const val DEBOUNCE_MS = 1_500L
}
