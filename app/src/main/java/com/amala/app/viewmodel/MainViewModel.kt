package com.amala.app.viewmodel

import android.app.Application
import android.os.Build
import com.amala.app.audio.AmlaAudioService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amala.app.audio.AudioController
import com.amala.app.audio.AudioDeviceDetector
import com.amala.app.audio.AudioEngine
import com.amala.app.data.AmlaPreferences
import com.amala.app.data.SharedPreferencesStore
import com.amala.app.model.AudioDevice
import com.amala.app.model.AudioDevices
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import com.amala.app.model.ProfileMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val devices: List<AudioDevice> = emptyList(),
        val activeDevice: AudioDevice? = null,
        val selectedDevice: AudioDevice? = null,
        val profile: EqualizerProfile = EqualizerProfile(Preset.FLAT, List(DEFAULT_BANDS) { 0 }),
        val bandCount: Int = DEFAULT_BANDS,
        val minLevel: Int = DEFAULT_MIN,
        val maxLevel: Int = DEFAULT_MAX,
        val frequencies: List<Int> = listOf(60, 230, 910, 3_600, 14_000),
        val autoApply: Boolean = true,
        val effectsEnabled: Boolean = true,
        val engineStatus: AudioEngine.Status? = null,
        val bassBoostSupported: Boolean = false,
        val virtualizerSupported: Boolean = false,
        val loudnessSupported: Boolean = false,
        val reverbSupported: Boolean = false,
        val limiterSupported: Boolean = false,
        val isRealme: Boolean = false,
        val keepAlive: Boolean = true,
        val autoGainProtection: Boolean = true,
        val abCompareActive: Boolean = false,
        val totalBoostDb: Float = 0f,
        val storedProfiles: List<Pair<String, EqualizerProfile>> = emptyList(),
        val message: String? = null,
    )

    private val preferences = AmlaPreferences(SharedPreferencesStore(application))
    private var detector: AudioDeviceDetector? = null
    private var manuallySelected = false
    private var lastAppliedDeviceId: String? = null
    private var profileBeforeCompare: EqualizerProfile? = null

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        _ui.value = UiState(
            autoApply = preferences.autoApply,
            effectsEnabled = AudioController.enabled,
            engineStatus = AudioController.status(),
            bassBoostSupported = AudioController.bassBoostSupported,
            virtualizerSupported = AudioController.virtualizerSupported,
            loudnessSupported = AudioController.loudnessSupported,
            reverbSupported = AudioController.reverbSupported,
            limiterSupported = AudioController.limiterSupported,
            isRealme = Build.MANUFACTURER.equals("realme", ignoreCase = true),
            keepAlive = preferences.keepAlive,
            autoGainProtection = preferences.autoGainProtection,
            totalBoostDb = ProfileMath.totalBoostDb(AudioController.profile),
            bandCount = AudioController.bandCount,
            minLevel = AudioController.minLevel,
            maxLevel = AudioController.maxLevel,
            frequencies = AudioController.frequencies(),
            profile = AudioController.profile,
            storedProfiles = preferences.storedProfiles(),
        )

        detector = AudioDeviceDetector(application) { devices -> onDevicesChanged(devices) }
        detector?.start()
    }

    private fun onDevicesChanged(devices: List<AudioDevice>) {
        val active = AudioDevices.activeDevice(devices)
        val selected = if (manuallySelected && _ui.value.selectedDevice != null) {
            _ui.value.selectedDevice
        } else {
            active
        }
        val target = selected ?: active
        val profile = target?.let { device ->
            val stored = preferences.loadProfile(device.id)
                ?: ProfileMath.defaultFor(device.category, AudioController.bandCount, AudioController.minLevel, AudioController.maxLevel)
            ProfileMath.fit(stored, AudioController.bandCount, AudioController.minLevel, AudioController.maxLevel)
        } ?: _ui.value.profile

        if (target?.id != lastAppliedDeviceId) {
            lastAppliedDeviceId = target?.id
            preferences.lastDeviceId = active?.id
            if (_ui.value.autoApply && target != null) {
                AudioController.applyLoaded(profile)
            }
        }

        _ui.update {
            it.copy(
                devices = devices,
                activeDevice = active,
                selectedDevice = selected,
                profile = profile,
                bandCount = AudioController.bandCount,
                minLevel = AudioController.minLevel,
                maxLevel = AudioController.maxLevel,
                frequencies = AudioController.frequencies(),
                engineStatus = AudioController.status(),
                bassBoostSupported = AudioController.bassBoostSupported,
                virtualizerSupported = AudioController.virtualizerSupported,
                loudnessSupported = AudioController.loudnessSupported,
                reverbSupported = AudioController.reverbSupported,
                limiterSupported = AudioController.limiterSupported,
                totalBoostDb = ProfileMath.totalBoostDb(profile),
            )
        }
    }

    fun selectDevice(device: AudioDevice) {
        manuallySelected = true
        lastAppliedDeviceId = null
        val stored = preferences.loadProfile(device.id)
            ?: ProfileMath.defaultFor(device.category, AudioController.bandCount, AudioController.minLevel, AudioController.maxLevel)
        _ui.update { it.copy(selectedDevice = device) }
        commit(stored)
        setMessage("Viewing ${device.displayName}")
    }

    fun selectActiveDevice() {
        manuallySelected = false
        lastAppliedDeviceId = null
        detector?.refresh()
    }

    fun setBand(index: Int, level: Int) {
        val state = _ui.value
        if (index !in 0 until state.bandCount) return
        val bands = state.profile.bands.toMutableList()
        while (bands.size < state.bandCount) bands.add(0)
        bands[index] = level.coerceIn(state.minLevel, state.maxLevel)
        commit(state.profile.copy(bands = bands, preset = Preset.CUSTOM, custom = true))
    }

    fun setPreset(preset: Preset) {
        val state = _ui.value
        val levels = ProfileMath.levels(preset, state.bandCount, state.minLevel, state.maxLevel)
        commit(state.profile.copy(bands = levels, preset = preset, custom = false))
        setMessage("${preset.label} preset applied")
    }

    fun setBassBoost(value: Int) {
        commit(_ui.value.profile.copy(bassBoost = value.coerceIn(0, 1_000), custom = true))
    }

    fun setVirtualizer(value: Int) {
        commit(_ui.value.profile.copy(virtualizer = value.coerceIn(0, 1_000), custom = true))
    }

    fun setLoudness(value: Int) {
        commit(_ui.value.profile.copy(loudness = value.coerceIn(0, 1_000), custom = true))
    }

    fun setPreamp(value: Int) {
        commit(
            _ui.value.profile.copy(
                preamp = value.coerceIn(ProfileMath.MIN_PREAMP, ProfileMath.MAX_PREAMP),
                custom = true,
            ),
        )
    }

    fun setLimiterEnabled(enabled: Boolean) {
        commit(_ui.value.profile.copy(limiterEnabled = enabled, custom = true))
    }

    fun setLimiterStrength(value: Int) {
        commit(_ui.value.profile.copy(limiterStrength = value.coerceIn(0, 1_000), custom = true))
    }

    fun setReverb(index: Int) {
        commit(_ui.value.profile.copy(reverb = index.coerceIn(0, 6), custom = true))
    }

    fun setEffectsEnabled(enabled: Boolean) {
        AudioController.setEnabled(enabled)
        _ui.update { it.copy(effectsEnabled = enabled, engineStatus = AudioController.status()) }
        setMessage(if (enabled) "Equalizer enabled" else "Equalizer bypassed")
    }

    fun setAutoApply(enabled: Boolean) {
        preferences.autoApply = enabled
        _ui.update { it.copy(autoApply = enabled) }
        if (enabled) {
            lastAppliedDeviceId = null
            detector?.refresh()
        }
        setMessage(if (enabled) "Auto-detect enabled" else "Auto-detect disabled")
    }

    fun saveCustom() {
        commit(_ui.value.profile.copy(custom = true))
        setMessage("Profile saved for ${_ui.value.selectedDevice?.displayName ?: "this device"}")
    }

    fun resetProfile() {
        val device = _ui.value.selectedDevice ?: return
        preferences.deleteProfile(device.id)
        commit(
            ProfileMath.defaultFor(
                device.category,
                AudioController.bandCount,
                AudioController.minLevel,
                AudioController.maxLevel,
            ),
        )
        setMessage("Default profile restored")
    }

    fun deleteProfile(deviceId: String) {
        preferences.deleteProfile(deviceId)
        if (_ui.value.selectedDevice?.id == deviceId) {
            resetProfile()
        } else {
            _ui.update { it.copy(storedProfiles = preferences.storedProfiles()) }
            setMessage("Saved profile deleted")
        }
    }

    fun applyStoredProfile(deviceId: String) {
        val profile = preferences.loadProfile(deviceId) ?: return
        val device = _ui.value.devices.firstOrNull { it.id == deviceId }
        if (device != null) {
            manuallySelected = true
            _ui.update { it.copy(selectedDevice = device) }
        }
        commit(profile)
        setMessage("Profile loaded")
    }

    fun setAutoGainProtection(enabled: Boolean) {
        preferences.autoGainProtection = enabled
        _ui.update { it.copy(autoGainProtection = enabled) }
        commit(_ui.value.profile)
        setMessage(if (enabled) "Auto gain protection on" else "Auto gain protection off")
    }

    /** Instantly swaps the live sound between the saved curve and a flat one. */
    fun toggleAbCompare() {
        val state = _ui.value
        if (state.abCompareActive) {
            val original = profileBeforeCompare ?: return
            AudioController.applyLoaded(original)
            profileBeforeCompare = null
            _ui.update {
                it.copy(
                    abCompareActive = false,
                    totalBoostDb = ProfileMath.totalBoostDb(original),
                )
            }
            setMessage("A/B off · your curve restored")
        } else {
            profileBeforeCompare = state.profile
            val flat = EqualizerProfile(Preset.FLAT, List(state.bandCount) { 0 }, enabled = true)
            AudioController.applyLoaded(flat)
            _ui.update { it.copy(abCompareActive = true, totalBoostDb = 0f) }
            setMessage("A/B on · flat curve (tap again to restore)")
        }
    }

    fun setKeepAlive(enabled: Boolean) {
        preferences.keepAlive = enabled
        _ui.update { it.copy(keepAlive = enabled) }
        if (enabled) {
            AmlaAudioService.start(getApplication())
            setMessage("Amla will keep EQ active across apps")
        } else {
            AmlaAudioService.stop(getApplication())
            setMessage("Background service stopped")
        }
    }

    fun refreshDevices() {
        detector?.refresh()
        setMessage("Devices refreshed")
    }

    fun reinitializeEffects() {
        AudioController.reinitialize()
        _ui.update {
            it.copy(
                bandCount = AudioController.bandCount,
                minLevel = AudioController.minLevel,
                maxLevel = AudioController.maxLevel,
                frequencies = AudioController.frequencies(),
                engineStatus = AudioController.status(),
                bassBoostSupported = AudioController.bassBoostSupported,
                virtualizerSupported = AudioController.virtualizerSupported,
                loudnessSupported = AudioController.loudnessSupported,
                reverbSupported = AudioController.reverbSupported,
                limiterSupported = AudioController.limiterSupported,
            )
        }
        setMessage(AudioController.status().detail)
    }

    fun clearMessage() {
        _ui.update { it.copy(message = null) }
    }

    private fun commit(profile: EqualizerProfile) {
        val base = profile.copy(enabled = true)
        val adjusted = if (_ui.value.autoGainProtection) {
            base.copy(preamp = ProfileMath.compensatedPreamp(base))
        } else {
            base
        }
        _ui.update {
            it.copy(
                profile = adjusted,
                abCompareActive = false,
                totalBoostDb = ProfileMath.totalBoostDb(adjusted),
            )
        }
        profileBeforeCompare = null
        AudioController.setProfile(adjusted, _ui.value.selectedDevice?.id)
        _ui.update {
            it.copy(
                storedProfiles = preferences.storedProfiles(),
                engineStatus = AudioController.status(),
            )
        }
    }

    private fun setMessage(message: String) {
        viewModelScope.launch { _ui.update { it.copy(message = message) } }
    }

    override fun onCleared() {
        detector?.stop()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_BANDS = 5
        private const val DEFAULT_MIN = -15_000
        private const val DEFAULT_MAX = 15_000
    }
}
