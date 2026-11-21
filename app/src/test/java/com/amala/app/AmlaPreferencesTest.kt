package com.amala.app

import com.amala.app.data.AmlaPreferences
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AmlaPreferencesTest {

    private lateinit var store: InMemoryStore
    private lateinit var preferences: AmlaPreferences

    @Before
    fun setUp() {
        store = InMemoryStore()
        preferences = AmlaPreferences(store)
    }

    @Test
    fun `settings have sensible defaults`() {
        assertTrue(preferences.autoApply)
        assertTrue(preferences.effectsEnabled)
        assertNull(preferences.lastDeviceId)
    }

    @Test
    fun `profile survives save and load`() {
        val profile = EqualizerProfile(
            preset = Preset.ROCK,
            bands = listOf(-1_500, 0, 2_500, 1_000, -500),
            bassBoost = 450,
            virtualizer = 200,
            loudness = 600,
            reverb = 3,
            enabled = true,
            custom = true,
        )
        preferences.saveProfile("BLUETOOTH:airpods", profile)
        assertEquals(profile, preferences.loadProfile("BLUETOOTH:airpods"))
    }

    @Test
    fun `deleting a profile removes only that device`() {
        val profile = EqualizerProfile(Preset.FLAT, listOf(0, 0, 0, 0, 0))
        preferences.saveProfile("WIRED:headphones", profile)
        preferences.saveProfile("USB_DAC:dac", profile)
        preferences.deleteProfile("WIRED:headphones")
        assertNull(preferences.loadProfile("WIRED:headphones"))
        assertEquals(profile, preferences.loadProfile("USB_DAC:dac"))
    }

    @Test
    fun `stored profiles lists every saved device`() {
        val profile = EqualizerProfile(Preset.POP, listOf(100, 200))
        preferences.saveProfile("USB_DAC:dac", profile)
        preferences.saveProfile("BLUETOOTH:speaker", profile)
        val ids = preferences.storedProfiles().map { it.first }
        assertEquals(listOf("BLUETOOTH:speaker", "USB_DAC:dac"), ids)
    }

    @Test
    fun `old saved profiles without loudness and reverb still load`() {
        store.putString(
            "profile:BLUETOOTH:old",
            "ROCK|1|0|300|100|100,200,300,400,500",
        )
        val loaded = preferences.loadProfile("BLUETOOTH:old")
        assertEquals(Preset.ROCK, loaded?.preset)
        assertEquals(0, loaded?.loudness)
        assertEquals(0, loaded?.reverb)
    }

    @Test
    fun `malformed saved data is ignored`() {
        store.putString("profile:broken", "not|a|valid|profile")
        assertNull(preferences.loadProfile("broken"))
        assertTrue(preferences.storedProfiles().isEmpty())
    }

    @Test
    fun `settings and last device persist`() {
        preferences.autoApply = false
        preferences.effectsEnabled = false
        preferences.lastDeviceId = "BLUETOOTH:speaker"
        assertFalse(preferences.autoApply)
        assertFalse(preferences.effectsEnabled)
        assertEquals("BLUETOOTH:speaker", preferences.lastDeviceId)
    }
}
