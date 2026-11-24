package com.amala.app.audio

import android.content.Context
import com.amala.app.data.AmlaPreferences
import com.amala.app.data.SharedPreferencesStore
import com.amala.app.ui.AmlaTileService
import com.amala.app.ui.AmlaWidget
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import com.amala.app.model.ProfileMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single owner of the audio effects so the UI, notification, widget, tile and
 * broadcast receiver all use the same engine and stay in sync.
 */
object AudioController {

    private lateinit var preferences: AmlaPreferences
    private lateinit var appContext: Context
    private val engine = AudioEngine()

    /** Incremented whenever audio state changes so observers can refresh. */
    private val _stateVersion = MutableStateFlow(0)
    val stateVersion: StateFlow<Int> = _stateVersion.asStateFlow()

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
    val limiterSupported: Boolean get() = engine.limiterSupported

    fun initialize(context: Context) {
        appContext = context.applicationContext
        preferences = AmlaPreferences(SharedPreferencesStore(appContext))
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
        notifyChanged()
    }

    fun frequencies(): List<Int> = engine.frequencies()

    fun status(): AudioEngine.Status = engine.status()

    /** Applies a profile and optionally persists it for the given device. */
    fun setProfile(profile: EqualizerProfile, deviceId: String? = null) {
        profileState = fit(profile)
        if (deviceId != null) preferences.saveProfile(deviceId, profileState)
        applyCurrent()
        notifyChanged()
    }

    /** Applies a loaded profile without overwriting what is saved for the device. */
    fun applyLoaded(profile: EqualizerProfile) {
        profileState = fit(profile)
        applyCurrent()
        notifyChanged()
    }

    fun setEnabled(enabled: Boolean) {
        enabledState = enabled
        if (::preferences.isInitialized) preferences.effectsEnabled = enabled
        applyCurrent()
        notifyChanged()
    }

    fun toggleEnabled() = setEnabled(!enabledState)

    /** Cycles through the built-in presets; used by notification/widget controls. */
    fun cyclePreset(direction: Int) {
        val current = profileState.preset
        val all = Preset.entries
        val next = all[(current.ordinal + direction + all.size) % all.size]
        val bands = ProfileMath.levels(next, bandCount, minLevel, maxLevel)
        setProfile(
            profileState.copy(preset = next, bands = bands, custom = false),
            preferences.lastDeviceId,
        )
    }

    /** Used by the notification bass up/down actions. */
    fun adjustBass(delta: Int) {
        setProfile(
            profileState.copy(
                bassBoost = (profileState.bassBoost + delta).coerceIn(0, 1_000),
                custom = true,
            ),
            preferences.lastDeviceId,
        )
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
        notifyChanged()
    }

    /** Reapplies the current profile without changing what is saved. */
    fun refresh() {
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

    private fun notifyChanged() {
        _stateVersion.value++
        if (!::appContext.isInitialized) return
        runCatching { AmlaWidget.update(appContext) }
        runCatching { AmlaTileService.requestUpdate(appContext) }
    }

    private const val DEFAULT_BANDS = 5
}
