package com.amala.app

import android.media.AudioFormat
import com.amala.app.model.AudioSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSignalTest {

    @Test
    fun `unknown values are shown as N A`() {
        assertEquals(AudioSignal.NA, AudioSignal.formatRate(null))
        assertEquals(AudioSignal.NA, AudioSignal.formatDepth(null))
        assertEquals(AudioSignal.NA, AudioSignal.codecName(null))
        assertEquals(AudioSignal.NA, AudioSignal.formatChannels(null))
        assertEquals(AudioSignal.NA, AudioSignal.formatBitrate(null))
    }

    @Test
    fun `rates format in kilohertz`() {
        assertEquals("44.1 kHz", AudioSignal.formatRate(44_100))
        assertEquals("96.0 kHz", AudioSignal.formatRate(96_000))
    }

    @Test
    fun `pcm encodings map to bit depth and codec`() {
        assertEquals(16, AudioSignal.bitDepth(AudioFormat.ENCODING_PCM_16BIT))
        assertEquals(24, AudioSignal.bitDepth(AudioFormat.ENCODING_PCM_FLOAT))
        assertEquals("PCM", AudioSignal.codecName(AudioFormat.ENCODING_PCM_16BIT))
        assertEquals("24-bit", AudioSignal.formatDepth(AudioFormat.ENCODING_PCM_FLOAT))
    }

    @Test
    fun `hi res requires both rate and depth`() {
        assertTrue(AudioSignal.isHiRes(96_000, AudioFormat.ENCODING_PCM_FLOAT))
        assertFalse(AudioSignal.isHiRes(48_000, AudioFormat.ENCODING_PCM_FLOAT))
        assertFalse(AudioSignal.isHiRes(96_000, AudioFormat.ENCODING_PCM_16BIT))
    }

    @Test
    fun `resampled needs both rates`() {
        assertTrue(AudioSignal.isResampled(44_100, 48_000))
        assertFalse(AudioSignal.isResampled(null, 48_000))
        assertFalse(AudioSignal.isResampled(48_000, 48_000))
    }

    @Test
    fun `usb ids are printed as hexadecimal`() {
        assertEquals("0x1234", AudioSignal.formatVendorId(0x1234))
    }
}
