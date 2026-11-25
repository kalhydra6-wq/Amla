package com.amala.app

import com.amala.app.model.SoundMoods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundMoodTest {

    @Test
    fun `all moods have unique keys and safe effect values`() {
        assertEquals(7, SoundMoods.all.size)
        assertEquals(7, SoundMoods.all.map { it.key }.distinct().size)
        SoundMoods.all.forEach { mood ->
            assertTrue(mood.bassBoost in 0..1_000)
            assertTrue(mood.virtualizer in 0..1_000)
            assertTrue(mood.loudness in 0..1_000)
            assertTrue(mood.reverb in 0..6)
        }
    }
}
