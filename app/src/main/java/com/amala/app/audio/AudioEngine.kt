package com.amala.app.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.amala.app.model.EqualizerProfile
import kotlin.math.pow

/**
 * Wraps Android's audio effects. Session 0 attaches the effects to the global
 * output mix so they apply to every app (music, video, games, navigation, ...)
 * rather than to a single audio session.
 */
class AudioEngine {

    companion object {
        private const val GLOBAL_SESSION = 0
        private const val DEFAULT_BANDS = 5
        private const val DEFAULT_MIN = -15_000
        private const val DEFAULT_MAX = 15_000
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    val ready: Boolean get() = equalizer != null
    val bassBoostSupported: Boolean get() = bassBoost != null
    val virtualizerSupported: Boolean get() = virtualizer != null

    val bandCount: Int
        get() = equalizer?.numberOfBands?.toInt()?.coerceAtLeast(1) ?: DEFAULT_BANDS

    val minLevel: Int
        get() = equalizer?.bandLevelRange?.getOrNull(0)?.toInt() ?: DEFAULT_MIN

    val maxLevel: Int
        get() = equalizer?.bandLevelRange?.getOrNull(1)?.toInt() ?: DEFAULT_MAX

    /** Initializes all supported effects. Each effect is independent so one failure is not fatal. */
    fun initialize(): Boolean {
        release()
        equalizer = runCatching { Equalizer(GLOBAL_SESSION, GLOBAL_SESSION) }.getOrNull()
        bassBoost = runCatching { BassBoost(GLOBAL_SESSION, GLOBAL_SESSION) }.getOrNull()
        virtualizer = runCatching { Virtualizer(GLOBAL_SESSION, GLOBAL_SESSION) }.getOrNull()
        return ready
    }

    /** Real band frequencies reported by the device, with a sensible fallback. */
    fun frequencies(): List<Int> = (0 until bandCount).map { index ->
        val frequency = try {
            equalizer?.getCenterFreq(index.toShort())
        } catch (ex: Exception) {
            null
        }
        if (frequency != null && frequency > 0) frequency else fallbackFrequency(index, bandCount)
    }

    fun apply(profile: EqualizerProfile) {
        val eq = equalizer ?: return
        runCatching { eq.enabled = profile.enabled }

        if (!profile.enabled) {
            runCatching {
                bassBoost?.enabled = false
                virtualizer?.enabled = false
            }
            return
        }

        val min = minLevel.toShort()
        val max = maxLevel.toShort()
        for (index in 0 until bandCount) {
            val level = (profile.bands.getOrNull(index) ?: 0).toShort().coerceIn(min, max)
            runCatching { eq.setBandLevel(index.toShort(), level) }
        }

        val bass = profile.bassBoost.coerceIn(0, 1_000)
        runCatching {
            bassBoost?.enabled = bass > 0
            if (bass > 0) bassBoost?.setStrength(bass.toShort())
        }

        val surround = profile.virtualizer.coerceIn(0, 1_000)
        runCatching {
            virtualizer?.enabled = surround > 0
            if (surround > 0) virtualizer?.setStrength(surround.toShort())
        }
    }

    fun previewBand(index: Int, level: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.setBandLevel(
                index.toShort(),
                level.toShort().coerceIn(minLevel.toShort(), maxLevel.toShort()),
            )
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    private fun fallbackFrequency(index: Int, count: Int): Int {
        if (count <= 1) return 1_000
        val ratio = 20.0.pow(index / (count - 1).toDouble())
        return (20 * ratio).toInt()
    }
}
