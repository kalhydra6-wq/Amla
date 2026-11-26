package com.amala.app

import com.amala.app.data.SoundEvent
import com.amala.app.data.SoundEventSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundEventSummaryTest {

    private fun event(device: String, app: String?, hour: Int) = SoundEvent(
        timestamp = 1L,
        deviceType = device,
        deviceId = device,
        appPackage = app,
        hour = hour,
        weekday = 2,
        changeKind = "band",
        bandIndex = 0,
        effectName = null,
        oldValue = 0f,
        newValue = 2f,
        presetName = "ROCK",
    )

    @Test
    fun `counts are grouped by device app and hour`() {
        val events = listOf(
            event("Bluetooth", "com.youtube", 22),
            event("Bluetooth", "com.youtube", 22),
            event("Speaker", null, 9),
        )
        assertEquals(mapOf("Bluetooth" to 2, "Speaker" to 1), SoundEventSummary.countByDevice(events))
        assertEquals(mapOf("com.youtube" to 2), SoundEventSummary.countByApp(events))
        assertEquals(mapOf(22 to 2, 9 to 1), SoundEventSummary.countByHour(events))
    }
}
