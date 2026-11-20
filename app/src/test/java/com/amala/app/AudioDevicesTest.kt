package com.amala.app

import android.media.AudioDeviceInfo
import com.amala.app.model.AudioDevice
import com.amala.app.model.AudioDevices
import com.amala.app.model.DeviceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioDevicesTest {

    private fun device(id: String, category: DeviceCategory) = AudioDevice(
        id = id,
        name = id,
        category = category,
        typeLabel = id,
    )

    @Test
    fun `framework types map to the right families`() {
        assertEquals(DeviceCategory.BLUETOOTH, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertEquals(DeviceCategory.BLUETOOTH, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertEquals(DeviceCategory.USB_DAC, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_USB_DEVICE))
        assertEquals(DeviceCategory.USB_DAC, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_USB_HEADSET))
        assertEquals(DeviceCategory.WIRED, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        assertEquals(DeviceCategory.SPEAKER, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertEquals(DeviceCategory.OTHER, AudioDevices.categoryFor(AudioDeviceInfo.TYPE_HDMI))
    }

    @Test
    fun `active device prefers Bluetooth then DAC then wired then speaker`() {
        val speaker = device("speaker", DeviceCategory.SPEAKER)
        val wired = device("wired", DeviceCategory.WIRED)
        val dac = device("dac", DeviceCategory.USB_DAC)
        val bluetooth = device("bt", DeviceCategory.BLUETOOTH)

        assertEquals(bluetooth, AudioDevices.activeDevice(listOf(speaker, wired, dac, bluetooth)))
        assertEquals(dac, AudioDevices.activeDevice(listOf(speaker, wired, dac)))
        assertEquals(wired, AudioDevices.activeDevice(listOf(speaker, wired)))
        assertEquals(speaker, AudioDevices.activeDevice(listOf(speaker)))
        assertNull(AudioDevices.activeDevice(emptyList()))
    }

    @Test
    fun `normalize removes duplicate ids`() {
        val first = device("bt:one", DeviceCategory.BLUETOOTH)
        val duplicate = device("bt:one", DeviceCategory.BLUETOOTH)
        assertEquals(1, AudioDevices.normalize(listOf(first, duplicate)).size)
    }

    @Test
    fun `ids are stable and sanitized`() {
        assertEquals(
            "BLUETOOTH:sony_wh_1000xm5",
            AudioDevices.makeId(DeviceCategory.BLUETOOTH, "Sony WH-1000XM5"),
        )
        assertEquals("SPEAKER:default", AudioDevices.makeId(DeviceCategory.SPEAKER, ""))
    }
}
