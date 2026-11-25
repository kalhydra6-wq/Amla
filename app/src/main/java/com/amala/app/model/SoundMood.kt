package com.amala.app.model

/** A ready-made, friendly sound mood built from existing EQ presets + effects. */
data class SoundMood(
    val key: String,
    val preset: Preset,
    val bassBoost: Int,
    val virtualizer: Int,
    val loudness: Int,
    val reverb: Int,
    val colorHex: Long,
)

object SoundMoods {
    val all = listOf(
        SoundMood("chai_time", Preset.ACOUSTIC, 250, 150, 150, 2, 0xFFB4703A),
        SoundMood("raat_drive", Preset.BASS_BOOST, 650, 300, 250, 0, 0xFF2F4A8A),
        SoundMood("gym_beast", Preset.ROCK, 800, 100, 350, 0, 0xFFD23B2A),
        SoundMood("baarish", Preset.JAZZ, 300, 350, 100, 4, 0xFF4A7FA5),
        SoundMood("padhai_focus", Preset.PODCAST, 100, 50, 120, 0, 0xFF4C9A2A),
        SoundMood("mehfil", Preset.POP, 550, 450, 300, 3, 0xFFC2185B),
        SoundMood("bhajan_shanti", Preset.CLASSICAL, 150, 250, 80, 5, 0xFF8A6BB0),
    )
}
