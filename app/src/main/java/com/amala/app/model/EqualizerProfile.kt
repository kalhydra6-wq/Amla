package com.amala.app.model

import java.util.Locale
import kotlin.math.floor
import kotlin.math.min

/**
 * EQ shapes are stored as five reference points in the range -1f..1f and
 * resampled to however many bands the device actually exposes.
 */
enum class Preset(val label: String, val shape: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    CUSTOM("Custom", listOf(0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Bass Boost", listOf(0.85f, 0.55f, 0.15f, -0.1f, -0.2f)),
    TREBLE("Treble", listOf(-0.2f, -0.1f, 0.1f, 0.55f, 0.85f)),
    VOCAL("Vocal", listOf(-0.25f, 0.1f, 0.65f, 0.4f, -0.1f)),
    ROCK("Rock", listOf(0.65f, 0.25f, -0.2f, 0.25f, 0.65f)),
    POP("Pop", listOf(0.25f, 0.5f, 0.7f, 0.3f, 0f)),
    JAZZ("Jazz", listOf(0.35f, 0f, 0.2f, 0.45f, 0.65f)),
    CLASSICAL("Classical", listOf(0.45f, 0.15f, -0.1f, 0.35f, 0.55f)),
    ELECTRONIC("Electronic", listOf(0.75f, 0.35f, -0.1f, 0.35f, 0.75f)),
    ACOUSTIC("Acoustic", listOf(0.25f, 0.3f, 0.45f, 0.3f, 0.15f)),
    PODCAST("Podcast", listOf(-0.45f, -0.1f, 0.55f, 0.7f, 0.3f));

    companion object {
        fun fromKey(key: String?): Preset = entries.firstOrNull { it.name == key } ?: FLAT
    }
}

/** A saved EQ configuration. Band levels and strengths use Android's raw units. */
data class EqualizerProfile(
    val preset: Preset,
    val bands: List<Int>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val reverb: Int = 0,
    val preamp: Int = 0,
    val limiterEnabled: Boolean = false,
    val limiterStrength: Int = 500,
    val enabled: Boolean = true,
    val custom: Boolean = false,
)

/** Reverb "room" options mapped to Android's PresetReverb indices. */
enum class ReverbPreset(val label: String, val androidPreset: Short) {
    OFF("Off", 0),
    SMALL_ROOM("Small room", 1),
    MEDIUM_ROOM("Medium room", 2),
    LARGE_ROOM("Large room", 3),
    MEDIUM_HALL("Medium hall", 4),
    LARGE_HALL("Large hall", 5),
    PLATE("Plate", 6);

    companion object {
        fun fromIndex(index: Int): ReverbPreset =
            entries.getOrNull(index) ?: OFF
    }
}

/** Pure, testable math used to turn preset shapes into real equalizer levels. */
object ProfileMath {
    const val MIN_PREAMP = -1_200
    const val MAX_PREAMP = 600

    /** Resamples [shape] to [size] bands using linear interpolation. */
    fun resample(shape: List<Float>, size: Int): List<Float> {
        val target = size.coerceAtLeast(1)
        if (shape.isEmpty()) return List(target) { 0f }
        if (shape.size == target) return shape
        if (target == 1) return listOf(shape.average().toFloat())

        return List(target) { index ->
            val position = index.toFloat() * (shape.size - 1) / (target - 1)
            val lower = floor(position).toInt()
            val upper = min(lower + 1, shape.size - 1)
            val fraction = position - lower
            shape[lower] * (1f - fraction) + shape[upper] * fraction
        }
    }

    /**
     * Converts a preset shape into band levels in millibels, clamped to the
     * range reported by the device's Equalizer.
     */
    fun levels(preset: Preset, bandCount: Int, minLevel: Int, maxLevel: Int): List<Int> {
        val min = minOf(minLevel, maxLevel)
        val max = maxOf(minLevel, maxLevel)
        return resample(preset.shape, bandCount).map { value ->
            val normalized = value.coerceIn(-1f, 1f)
            val raw = if (normalized >= 0f) normalized * max else normalized * -min
            raw.toInt().coerceIn(min, max)
        }
    }

    fun defaultPreset(category: DeviceCategory): Preset = when (category) {
        DeviceCategory.BLUETOOTH -> Preset.BASS_BOOST
        DeviceCategory.USB_DAC -> Preset.FLAT
        DeviceCategory.WIRED -> Preset.ACOUSTIC
        DeviceCategory.SPEAKER -> Preset.VOCAL
        DeviceCategory.OTHER -> Preset.FLAT
    }

    fun defaultFor(
        category: DeviceCategory,
        bandCount: Int,
        minLevel: Int,
        maxLevel: Int,
    ): EqualizerProfile {
        val preset = defaultPreset(category)
        return EqualizerProfile(
            preset = preset,
            bands = levels(preset, bandCount, minLevel, maxLevel),
            enabled = true,
            custom = false,
        )
    }

    /** Adapts a stored profile to a device with a different number of bands. */
    fun fit(profile: EqualizerProfile, bandCount: Int, minLevel: Int, maxLevel: Int): EqualizerProfile =
        profile.copy(
            bands = resample(profile.bands.map { it.toFloat() }, bandCount)
                .map { it.toInt().coerceIn(minOf(minLevel, maxLevel), maxOf(minLevel, maxLevel)) },
            bassBoost = profile.bassBoost.coerceIn(0, 1000),
            virtualizer = profile.virtualizer.coerceIn(0, 1000),
            loudness = profile.loudness.coerceIn(0, 1000),
            reverb = profile.reverb.coerceIn(0, ReverbPreset.entries.size - 1),
            preamp = profile.preamp.coerceIn(MIN_PREAMP, MAX_PREAMP),
            limiterEnabled = profile.limiterEnabled,
            limiterStrength = profile.limiterStrength.coerceIn(0, 1000),
        )

    /** Strongest single boost from the EQ bands or bass boost, in dB. */
    fun maxBoostDb(profile: EqualizerProfile): Float {
        val bandBoost = (profile.bands.maxOfOrNull { it } ?: 0).coerceAtLeast(0) / 100f
        val bassBoost = profile.bassBoost.coerceAtLeast(0) / 100f
        return maxOf(bandBoost, bassBoost)
    }

    /** Auto gain: set preamp to minus half of the strongest boost, within limits. */
    fun compensatedPreamp(profile: EqualizerProfile): Int {
        val compensation = (maxBoostDb(profile) / 2f * 100f).toInt()
        return (-compensation).coerceIn(MIN_PREAMP, MAX_PREAMP)
    }

    /** Sum of all positive band boosts plus bass boost, in dB. */
    fun totalBoostDb(profile: EqualizerProfile): Float {
        val bands = profile.bands.filter { it > 0 }.sum() / 100f
        return bands + profile.bassBoost.coerceAtLeast(0) / 100f
    }

    fun formatLevel(level: Int): String = String.format(Locale.US, "%+.1f dB", level / 100f)

    /** Android's Equalizer.getCenterFreq() returns millihertz. */
    fun hzFromMilliHz(milliHz: Int): Int = milliHz / 1_000

    fun formatFrequency(hz: Int): String =
        if (hz >= 1000) String.format(Locale.US, "%.1f kHz", hz / 1000f) else "$hz Hz"
}
