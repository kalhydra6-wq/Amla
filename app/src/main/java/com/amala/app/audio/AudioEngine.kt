package com.amala.app.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.ReverbPreset
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
    private var loudness: LoudnessEnhancer? = null
    private var reverb: PresetReverb? = null
    private var dynamics: DynamicsProcessing? = null
    private val sessionEqualizers = mutableMapOf<Int, Equalizer>()

    val ready: Boolean get() = equalizer != null
    val bassBoostSupported: Boolean get() = bassBoost != null
    val virtualizerSupported: Boolean get() = virtualizer != null
    val loudnessSupported: Boolean get() = loudness != null
    val reverbSupported: Boolean get() = reverb != null
    val limiterSupported: Boolean get() = dynamics != null

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
        loudness = runCatching { LoudnessEnhancer(GLOBAL_SESSION) }.getOrNull()
        reverb = runCatching { PresetReverb(EFFECT_PRIORITY, GLOBAL_SESSION) }.getOrNull()
        dynamics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { createLimiter() }.getOrNull()
        } else {
            null
        }
        return ready
    }

    /** Real band frequencies reported by the device, with a sensible fallback. */
    fun frequencies(): List<Int> = (0 until bandCount).map { index ->
        val milliHz = try {
            equalizer?.getCenterFreq(index.toShort())
        } catch (ex: Exception) {
            null
        }
        // getCenterFreq reports millihertz (60 Hz == 60_000 mHz).
        if (milliHz != null && milliHz > 0) milliHz / 1_000 else fallbackFrequency(index, bandCount)
    }

    fun apply(profile: EqualizerProfile) {
        equalizer?.let { applyToEffect(it, profile) }
        sessionEqualizers.values.forEach { effect -> applyToEffect(effect, profile) }
        applyAuxiliaryEffects(profile)
        applyLimiter(profile)
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
        runCatching { loudness?.release() }
        runCatching { reverb?.release() }
        runCatching { dynamics?.release() }
        sessionEqualizers.values.forEach { runCatching { it.release() } }
        sessionEqualizers.clear()
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        reverb = null
        dynamics = null
    }

    private fun applyToEffect(effect: Equalizer, profile: EqualizerProfile) {
        runCatching { effect.setEnabled(profile.enabled) }
        if (!profile.enabled) return
        val min = minLevel.toShort()
        val max = maxLevel.toShort()
        for (index in 0 until bandCount) {
            val base = profile.bands.getOrNull(index) ?: 0
            val level = (base + profile.preamp).toShort().coerceIn(min, max)
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

        // Loudness compensates for the perceived volume drop caused by EQ processing.
        val boost = profile.loudness.coerceIn(0, 1_000)
        runCatching {
            loudness?.setEnabled(profile.enabled && boost > 0)
            if (profile.enabled && boost > 0) loudness?.setTargetGain(boost * 2)
        }

        val room = ReverbPreset.fromIndex(profile.reverb)
        runCatching {
            reverb?.setPreset(room.androidPreset)
            reverb?.setEnabled(profile.enabled && room != ReverbPreset.OFF)
        }
    }

    /** Creates an insert effect whose only active stage is the limiter. */
    private fun createLimiter(): DynamicsProcessing {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2,
            false, 0,
            false, 0,
            false, 0,
            true,
        ).build()
        return DynamicsProcessing(EFFECT_PRIORITY, GLOBAL_SESSION, config)
    }

    private fun applyLimiter(profile: EqualizerProfile) {
        val effect = dynamics ?: return
        val active = profile.enabled && profile.limiterEnabled && profile.limiterStrength > 0
        runCatching {
            effect.setEnabled(active)
            if (active) {
                val amount = profile.limiterStrength.coerceIn(0, 1_000) / 1_000f
                val threshold = -3f - 9f * amount
                val ratio = 6f + 14f * amount
                val limiter = DynamicsProcessing.Limiter(
                    true, true, 0, 1f, 100f, ratio, threshold, 0f,
                )
                effect.setLimiterAllChannelsTo(limiter)
            }
        }
    }

    private fun fallbackFrequency(index: Int, count: Int): Int {
        if (count <= 1) return 1_000
        val ratio = 20.0.pow(index / (count - 1).toDouble())
        return (20 * ratio).toInt()
    }
}
