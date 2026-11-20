package com.amala.app.model

/**
 * Broad family an output device belongs to. Each family can keep its own EQ profile.
 */
enum class DeviceCategory(val displayName: String, val description: String) {
    BLUETOOTH("Bluetooth", "Wireless headphones and speakers"),
    USB_DAC("USB DAC", "USB audio adapters and DACs"),
    WIRED("Wired", "3.5 mm headphones and headsets"),
    SPEAKER("Speaker", "Built-in device speaker"),
    OTHER("Other", "Dock, HDMI, line-out or network audio");

    companion object {
        fun fromKey(key: String?): DeviceCategory =
            entries.firstOrNull { it.name == key } ?: OTHER
    }
}
