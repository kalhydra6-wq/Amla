package com.amala.app.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.amala.app.model.EqualizerProfile
import kotlin.math.pow

/**
 * Wraps Android's audio effects. Session 0 targets the global output mix so the
 * EQ applies to all apps. A priority above 0 is used so Amla takes control of
 * the effect engine when another application currently owns it.
 */
class AudioEngine {

    data class Status(
        val ready: Boolean,
        val hasControl: Boolean,
        val enabled: Boolean,
        val attachedSessions: Int,
        val detail: String,
    )

    companion object {
        private const val GLOBAL_SESSION = 0
        private const val EFFECT_PRIORITY = 10
        private const val DEFAULT_BANDS = 5
        private const val DEFAULT_MIN = -15_000
        private const val DEFAULT_MAX = 15_000
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private val sessionEqualizers = mutableMapOf<Int, Equalizer>()

    val ready: Boolean get() = equalizer != null
    val bassBoostSupported: Boolean get() = bassBoost != null
    val virtualizerSupported: Boolean get() = virtualizer != null

    val bandCount: Int
        get() = equalizer?.numberOfBands?.toInt()?.coerceAtLeast(1) ?: DEFAULT_BANDS

    val minLevel: Int
        get() = equalizer?.bandLevelRange?.getOrNull(0)?.toInt() ?: DEFAULT_MIN

    val maxLevel: Int
        get() = equalizer?.bandLevelRange?.getOrNull(1)?.toInt() ?: DEFAULT_MAX

    /** Initializes global effects. Each effect is independent so one failure is not fatal. */
    fun initialize(): Boolean {
        release()
        equalizer = runCatching { Equalizer(EFFECT_PRIORITY, GLOBAL_SESSION) }.getOrNull()
        bassBoost = runCatching { BassBoost(EFFECT_PRIORITY, GLOBAL_SESSION) }.getOrNull()
        virtualizer = runCatching { Virtualizer(EFFECT_PRIORITY, GLOBAL_SESSION) }.getOrNull()
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
        equalizer?.let { applyToEffect(it, profile) }
        sessionEqualizers.values.forEach { effect -> applyToEffect(effect, profile) }
        applyAuxiliaryEffects(profile)
    }

    /** Attaches the EQ to a media app's own audio session (effect-control intent). */
    fun attachSession(sessionId: Int, profile: EqualizerProfile) {
        if (sessionId == GLOBAL_SESSION) return
        val effect = sessionEqualizers[sessionId] ?: runCatching {
            Equalizer(EFFECT_PRIORITY, sessionId)
        }.getOrNull() ?: return
        sessionEqualizers[sessionId] = effect
        applyToEffect(effect, profile)
    }

    fun detachSession(sessionId: Int) {
        sessionEqualizers.remove(sessionId)?.let { runCatching { it.release() } }
    }

    fun status(): Status {
        val eq = equalizer
        val hasControl = eq?.let { runCatching { it.hasControl() }.getOrDefault(false) } ?: false
        val isEnabled = eq?.let { runCatching { it.enabled }.getOrDefault(false) } ?: false
        val detail = when {
            eq == null -> "Effect engine unavailable on this device"
            !hasControl -> "Another app controls the equalizer — close it and reinitialize"
            !isEnabled -> "Equalizer is bypassed"
            sessionEqualizers.isNotEmpty() -> "Active · ${sessionEqualizers.size} app session(s) attached"
            else -> "Active · global output mix"
        }
        return Status(eq != null, hasControl, isEnabled, sessionEqualizers.size, detail)
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        sessionEqualizers.values.forEach { runCatching { it.release() } }
        sessionEqualizers.clear()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    private fun applyToEffect(effect: Equalizer, profile: EqualizerProfile) {
        runCatching { effect.setEnabled(profile.enabled) }
        if (!profile.enabled) return
        val min = minLevel.toShort()
        val max = maxLevel.toShort()
        for (index in 0 until bandCount) {
            val level = (profile.bands.getOrNull(index) ?: 0).toShort().coerceIn(min, max)
            runCatching { effect.setBandLevel(index.toShort(), level) }
        }
    }

    private fun applyAuxiliaryEffects(profile: EqualizerProfile) {
        val bass = profile.bassBoost.coerceIn(0, 1_000)
        runCatching {
            bassBoost?.setEnabled(profile.enabled && bass > 0)
            if (profile.enabled && bass > 0) bassBoost?.setStrength(bass.toShort())
        }
        val surround = profile.virtualizer.coerceIn(0, 1_000)
        runCatching {
            virtualizer?.setEnabled(profile.enabled && surround > 0)
            if (profile.enabled && surround > 0) virtualizer?.setStrength(surround.toShort())
        }
    }

    private fun fallbackFrequency(index: Int, count: Int): Int {
        if (count <= 1) return 1_000
        val ratio = 20.0.pow(index / (count - 1).toDouble())
        return (20 * ratio).toInt()
    }
}
