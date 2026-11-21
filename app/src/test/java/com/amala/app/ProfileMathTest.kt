package com.amala.app

import com.amala.app.model.DeviceCategory
import com.amala.app.model.EqualizerProfile
import com.amala.app.model.Preset
import com.amala.app.model.ProfileMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMathTest {

    @Test
    fun `resample always returns requested number of bands`() {
        listOf(1, 2, 5, 10, 31).forEach { count ->
            assertEquals(count, ProfileMath.resample(Preset.ROCK.shape, count).size)
        }
    }

    @Test
    fun `resample interpolates between reference points`() {
        val result = ProfileMath.resample(listOf(0f, 10f), 3)
        assertEquals(0f, result[0], 0.001f)
        assertEquals(5f, result[1], 0.001f)
        assertEquals(10f, result[2], 0.001f)
    }

    @Test
    fun `flat preset produces silence across all bands`() {
        val levels = ProfileMath.levels(Preset.FLAT, 10, -15_000, 15_000)
        assertTrue(levels.all { it == 0 })
    }

    @Test
    fun `every preset stays inside hardware band limits`() {
        Preset.entries.forEach { preset ->
            listOf(1, 5, 10, 31).forEach { count ->
                val levels = ProfileMath.levels(preset, count, -12_000, 9_000)
                assertTrue("${preset.name}/$count -> $levels", levels.all { it in -12_000..9_000 })
            }
        }
    }

    @Test
    fun `bass preset boosts low frequencies more than high ones`() {
        val levels = ProfileMath.levels(Preset.BASS_BOOST, 5, -15_000, 15_000)
        assertTrue(levels[0] > levels[4])
    }

    @Test
    fun `default presets match output category`() {
        assertEquals(Preset.BASS_BOOST, ProfileMath.defaultPreset(DeviceCategory.BLUETOOTH))
        assertEquals(Preset.FLAT, ProfileMath.defaultPreset(DeviceCategory.USB_DAC))
        assertEquals(Preset.ACOUSTIC, ProfileMath.defaultPreset(DeviceCategory.WIRED))
        assertEquals(Preset.VOCAL, ProfileMath.defaultPreset(DeviceCategory.SPEAKER))
    }

    @Test
    fun `fit adapts band count and clamps stored values`() {
        val original = EqualizerProfile(Preset.CUSTOM, listOf(20_000, -20_000), 5_000, -5_000)
        val fitted = ProfileMath.fit(original, 4, -15_000, 15_000)
        assertEquals(4, fitted.bands.size)
        assertTrue(fitted.bands.all { it in -15_000..15_000 })
        assertEquals(1_000, fitted.bassBoost)
        assertEquals(0, fitted.virtualizer)
    }

    @Test
    fun `formatters produce readable values`() {
        assertEquals("+3.5 dB", ProfileMath.formatLevel(350))
        assertEquals("-2.0 dB", ProfileMath.formatLevel(-200))
        assertEquals("1.0 kHz", ProfileMath.formatFrequency(1_000))
        assertEquals("250 Hz", ProfileMath.formatFrequency(250))
    }

    @Test
    fun `millihertz from Android is converted to hertz`() {
        assertEquals(60, ProfileMath.hzFromMilliHz(60_000))
        assertEquals(3_600, ProfileMath.hzFromMilliHz(3_600_000))
        assertEquals("3.6 kHz", ProfileMath.formatFrequency(ProfileMath.hzFromMilliHz(3_600_000)))
    }
}
