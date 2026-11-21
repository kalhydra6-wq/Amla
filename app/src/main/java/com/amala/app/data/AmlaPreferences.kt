package com.amala.app.data

import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import com.amala.app.model.ReverbPreset

/** Stores one EQ profile per output device plus global app settings. */
class AmlaPreferences(private val store: KeyValueStore) {

    var autoApply: Boolean
        get() = store.getBoolean(KEY_AUTO_APPLY, true)
        set(value) = store.putBoolean(KEY_AUTO_APPLY, value)

    var effectsEnabled: Boolean
        get() = store.getBoolean(KEY_EFFECTS_ENABLED, true)
        set(value) = store.putBoolean(KEY_EFFECTS_ENABLED, value)

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
        )
    }

    companion object {
        private const val PROFILE_PREFIX = "profile:"
        private const val KEY_AUTO_APPLY = "setting:auto_apply"
        private const val KEY_EFFECTS_ENABLED = "setting:effects_enabled"
        private const val KEY_KEEP_ALIVE = "setting:keep_alive"
        private const val KEY_LAST_DEVICE = "last_device_id"
    }
}
