package com.amala.app.audio

import android.content.Context
import com.amala.app.data.AmlaPreferences
import com.amala.app.data.SharedPreferencesStore
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import com.amala.app.model.ProfileMath

/**
 * Single owner of the audio effects so both the UI and the broadcast receiver
 * (which also runs when the app is in the background) use the same engine.
 */
object AudioController {

    private lateinit var preferences: AmlaPreferences
    private val engine = AudioEngine()

    @Volatile
    private var profileState: EqualizerProfile = EqualizerProfile(Preset.FLAT, List(DEFAULT_BANDS) { 0 })

    @Volatile
    private var enabledState: Boolean = true

    val profile: EqualizerProfile get() = profileState
    val enabled: Boolean get() = enabledState
    val ready: Boolean get() = engine.ready
    val bandCount: Int get() = engine.bandCount
    val minLevel: Int get() = engine.minLevel
    val maxLevel: Int get() = engine.maxLevel
    val bassBoostSupported: Boolean get() = engine.bassBoostSupported
    val virtualizerSupported: Boolean get() = engine.virtualizerSupported
    val loudnessSupported: Boolean get() = engine.loudnessSupported
    val reverbSupported: Boolean get() = engine.reverbSupported

    fun initialize(context: Context) {
        preferences = AmlaPreferences(SharedPreferencesStore(context.applicationContext))
        enabledState = preferences.effectsEnabled
        engine.initialize()
        val stored = preferences.lastDeviceId?.let { preferences.loadProfile(it) }
        profileState = fit(
            stored ?: EqualizerProfile(
                preset = Preset.FLAT,
                bands = ProfileMath.levels(Preset.FLAT, bandCount, minLevel, maxLevel),
            ),
        )
        applyCurrent()
    }

    fun frequencies(): List<Int> = engine.frequencies()

    fun status(): AudioEngine.Status = engine.status()

    /** Applies a profile and optionally persists it for the given device. */
    fun setProfile(profile: EqualizerProfile, deviceId: String? = null) {
        profileState = fit(profile)
        if (deviceId != null) preferences.saveProfile(deviceId, profileState)
        applyCurrent()
    }

    /** Applies a loaded profile without overwriting what is saved for the device. */
    fun applyLoaded(profile: EqualizerProfile) {
        profileState = fit(profile)
        applyCurrent()
    }

    fun setEnabled(enabled: Boolean) {
        enabledState = enabled
        if (::preferences.isInitialized) preferences.effectsEnabled = enabled
        applyCurrent()
    }

    fun onSessionOpened(sessionId: Int) {
        engine.attachSession(sessionId, profileState.copy(enabled = enabledState))
    }

    fun onSessionClosed(sessionId: Int) {
        engine.detachSession(sessionId)
    }

    fun reinitialize() {
        engine.initialize()
        applyCurrent()
    }

    fun release() {
        engine.release()
    }

    private fun fit(profile: EqualizerProfile): EqualizerProfile =
        ProfileMath.fit(profile, bandCount, minLevel, maxLevel)

    private fun applyCurrent() {
        engine.apply(profileState.copy(enabled = enabledState))
    }

    private const val DEFAULT_BANDS = 5
}
