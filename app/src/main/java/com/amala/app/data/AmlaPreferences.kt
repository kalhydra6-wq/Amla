package com.amala.app.data

import com.amala.app.model.BackgroundMode
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import com.amala.app.model.ProfileMath
import com.amala.app.model.ReverbPreset
import com.amala.app.model.ThemeMode

/** Stores one EQ profile per output device plus global app settings. */
class AmlaPreferences(private val store: KeyValueStore) {

    /** Existing installs keep today's Always-on behaviour; fresh installs use Smart. */
    fun migrateBackgroundOnce() {
        if (store.getInt(KEY_BACKGROUND_MIGRATION_VERSION, 0) >= BACKGROUND_MIGRATION_VERSION) return
        val existingInstall = store.keys().any { it !in setOf(KEY_BACKGROUND_MIGRATION_VERSION) }
        store.putString(
            KEY_BACKGROUND_MODE,
            if (existingInstall) BackgroundMode.ALWAYS.name else BackgroundMode.SMART.name,
        )
        store.putInt(KEY_BACKGROUND_MIGRATION_VERSION, BACKGROUND_MIGRATION_VERSION)
    }

    var backgroundMode: BackgroundMode
        get() = BackgroundMode.fromKey(store.getString(KEY_BACKGROUND_MODE, null))
        set(value) = store.putString(KEY_BACKGROUND_MODE, value.name)

    var smartIdleMinutes: Int
        get() = store.getInt(KEY_SMART_IDLE_MINUTES, 2).takeIf { it in listOf(1, 2, 5, 10) } ?: 2
        set(value) = store.putInt(KEY_SMART_IDLE_MINUTES, value.takeIf { it in listOf(1, 2, 5, 10) } ?: 2)

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(store.getString(KEY_THEME_MODE, null))
        set(value) = store.putString(KEY_THEME_MODE, value.name)

    var accentColor: Int
        get() = store.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT)
        set(value) = store.putInt(KEY_ACCENT_COLOR, value)

    /** One-time, versioned migration. Never touches a colour the user has saved. */
    fun migrateThemeOnce() {
        if (store.getInt(KEY_THEME_MIGRATION_VERSION, 0) >= THEME_MIGRATION_VERSION) return
        if (KEY_ACCENT_COLOR !in store.keys()) store.putInt(KEY_ACCENT_COLOR, AMLA_GREEN)
        if (KEY_THEME_MODE !in store.keys()) store.putString(KEY_THEME_MODE, ThemeMode.DARK.name)
        store.putInt(KEY_THEME_MIGRATION_VERSION, THEME_MIGRATION_VERSION)
    }

    fun resetTheme() {
        accentColor = AMLA_GREEN
        themeMode = ThemeMode.DARK
    }

    var signalChainEnabled: Boolean
        get() = store.getBoolean(KEY_SIGNAL_CHAIN, true)
        set(value) = store.putBoolean(KEY_SIGNAL_CHAIN, value)

    var autoApply: Boolean
        get() = store.getBoolean(KEY_AUTO_APPLY, true)
        set(value) = store.putBoolean(KEY_AUTO_APPLY, value)

    var effectsEnabled: Boolean
        get() = store.getBoolean(KEY_EFFECTS_ENABLED, true)
        set(value) = store.putBoolean(KEY_EFFECTS_ENABLED, value)

    var softStartEnabled: Boolean
        get() = store.getBoolean(KEY_SOFT_START, true)
        set(value) = store.putBoolean(KEY_SOFT_START, value)

    var softStartSeconds: Int
        get() = store.getInt(KEY_SOFT_START_SECONDS, 2).takeIf { it in listOf(1, 2, 3, 5) } ?: 2
        set(value) = store.putInt(KEY_SOFT_START_SECONDS, value.takeIf { it in listOf(1, 2, 3, 5) } ?: 2)

    var autoGainProtection: Boolean
        get() = store.getBoolean(KEY_AUTO_GAIN, true)
        set(value) = store.putBoolean(KEY_AUTO_GAIN, value)

    var keepAlive: Boolean
        get() = store.getBoolean(KEY_KEEP_ALIVE, true)
        set(value) = store.putBoolean(KEY_KEEP_ALIVE, value)

    var lastDeviceId: String?
        get() = store.getString(KEY_LAST_DEVICE, null)
        set(value) {
            if (value == null) store.remove(KEY_LAST_DEVICE) else store.putString(KEY_LAST_DEVICE, value)
        }

    fun loadProfile(deviceId: String): EqualizerProfile? =
        store.getString(profileKey(deviceId), null)?.let(::decode)

    fun saveProfile(deviceId: String, profile: EqualizerProfile) {
        store.putString(profileKey(deviceId), encode(profile))
    }

    fun deleteProfile(deviceId: String) {
        store.remove(profileKey(deviceId))
    }

    fun storedProfiles(): List<Pair<String, EqualizerProfile>> =
        store.keys()
            .filter { it.startsWith(PROFILE_PREFIX) }
            .sorted()
            .mapNotNull { key -> decode(store.getString(key, null) ?: "")?.let { key.removePrefix(PROFILE_PREFIX) to it } }

    private fun profileKey(deviceId: String) = "$PROFILE_PREFIX$deviceId"

    private fun encode(profile: EqualizerProfile): String {
        val bands = profile.bands.joinToString(",") { it.toString() }
        return listOf(
            profile.preset.name,
            if (profile.enabled) "1" else "0",
            if (profile.custom) "1" else "0",
            profile.bassBoost.coerceIn(0, 1_000).toString(),
            profile.virtualizer.coerceIn(0, 1_000).toString(),
            bands,
            profile.loudness.coerceIn(0, 1_000).toString(),
            profile.reverb.coerceIn(0, ReverbPreset.entries.size - 1).toString(),
            profile.preamp.coerceIn(ProfileMath.MIN_PREAMP, ProfileMath.MAX_PREAMP).toString(),
            if (profile.limiterEnabled) "1" else "0",
            profile.limiterStrength.coerceIn(0, 1_000).toString(),
        ).joinToString("|")
    }

    private fun decode(raw: String): EqualizerProfile? {
        val parts = raw.split('|')
        // Files saved by older versions have only 6 fields; loudness and reverb are optional.
        if (parts.size < 6) return null
        val bands = parts[5].split(',').mapNotNull { it.toIntOrNull() }
        if (bands.isEmpty()) return null
        return EqualizerProfile(
            preset = Preset.fromKey(parts[0]),
            enabled = parts[1] == "1",
            custom = parts[2] == "1",
            bassBoost = parts[3].toIntOrNull()?.coerceIn(0, 1_000) ?: 0,
            virtualizer = parts[4].toIntOrNull()?.coerceIn(0, 1_000) ?: 0,
            bands = bands,
            loudness = parts.getOrNull(6)?.toIntOrNull()?.coerceIn(0, 1_000) ?: 0,
            reverb = parts.getOrNull(7)?.toIntOrNull()?.coerceIn(0, ReverbPreset.entries.size - 1) ?: 0,
            preamp = parts.getOrNull(8)?.toIntOrNull()
                ?.coerceIn(ProfileMath.MIN_PREAMP, ProfileMath.MAX_PREAMP) ?: 0,
            limiterEnabled = parts.getOrNull(9) == "1",
            limiterStrength = parts.getOrNull(10)?.toIntOrNull()?.coerceIn(0, 1_000) ?: 500,
        )
    }

    companion object {
        private const val PROFILE_PREFIX = "profile:"
        private const val KEY_AUTO_APPLY = "setting:auto_apply"
        private const val KEY_EFFECTS_ENABLED = "setting:effects_enabled"
        private const val KEY_KEEP_ALIVE = "setting:keep_alive"
        private const val KEY_AUTO_GAIN = "setting:auto_gain"
        private const val KEY_SOFT_START = "setting:soft_start"
        private const val KEY_SOFT_START_SECONDS = "setting:soft_start_seconds"
        private const val KEY_LAST_DEVICE = "last_device_id"
        private const val KEY_THEME_MODE = "appearance:theme_mode"
        private const val KEY_ACCENT_COLOR = "appearance:accent_color"
        private const val KEY_SIGNAL_CHAIN = "appearance:signal_chain"
        private const val KEY_THEME_MIGRATION_VERSION = "appearance:theme_migration_version"
        private const val KEY_BACKGROUND_MODE = "background:mode"
        private const val KEY_SMART_IDLE_MINUTES = "background:smart_idle_minutes"
        private const val KEY_BACKGROUND_MIGRATION_VERSION = "background:migration_version"
        private const val BACKGROUND_MIGRATION_VERSION = 1
        const val DEFAULT_ACCENT = 0xFF007C70.toInt()
        const val AMLA_GREEN = 0xFF4C9A2A.toInt()
        const val AMLA_PURPLE = 0xFF6D4BC4.toInt()
        private const val THEME_MIGRATION_VERSION = 1
    }
}
