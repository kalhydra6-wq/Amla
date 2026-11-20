package com.amala.app.model

import android.media.AudioDeviceInfo
import android.os.Build

/** A physical output device discovered by Android's audio framework. */
data class AudioDevice(
    val id: String,
    val name: String,
    val category: DeviceCategory,
    val typeLabel: String,
    val address: String? = null,
) {
    val displayName: String
        get() = name.ifBlank { category.displayName }
}

/**
 * Pure helpers for classifying, identifying and prioritizing output devices.
 * These functions contain no Android framework calls so they can be unit tested.
 */
object AudioDevices {

    /** Converts a framework device into an app model. Returns null for input devices. */
    fun fromInfo(info: AudioDeviceInfo): AudioDevice? {
        if (!info.isSink) return null

        val category = categoryFor(info.type)
        val name = info.productName?.toString()?.takeIf { it.isNotBlank() }
            ?: fallbackName(info.type, category)
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.address
        } else {
            null
        }

        return AudioDevice(
            id = makeId(category, address ?: name),
            name = name,
            category = category,
            typeLabel = typeLabel(info.type),
            address = address,
        )
    }

    fun categoryFor(type: Int): DeviceCategory = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> DeviceCategory.BLUETOOTH

        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> DeviceCategory.USB_DAC

        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> DeviceCategory.WIRED

        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> DeviceCategory.SPEAKER

        else -> DeviceCategory.OTHER
    }

    fun typeLabel(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "LE Audio headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "LE Audio speaker"
        AudioDeviceInfo.TYPE_BLE_HEARING_AID -> "LE hearing aid"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "LE Audio broadcast"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio device"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB accessory"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "Built-in speaker (safe)"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI output"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
        AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI eARC"
        AudioDeviceInfo.TYPE_DOCK -> "Dock output"
        AudioDeviceInfo.TYPE_DOCK_ANALOG -> "Analog dock output"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "Analog line out"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Digital line out"
        AudioDeviceInfo.TYPE_AUX_LINE -> "Auxiliary line"
        AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
        AudioDeviceInfo.TYPE_IP -> "Network audio"
        AudioDeviceInfo.TYPE_BUS -> "Bus output"
        AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing aid"
        else -> "Audio output"
    }

    /** Removes duplicates and orders devices from most to least likely to be active. */
    fun normalize(devices: List<AudioDevice>): List<AudioDevice> =
        devices.distinctBy { it.id }.sortedBy { priority(it.category) }

    /**
     * Chooses the device Android is most likely routing audio to right now.
     * Built-in speakers are always present, so they are ranked last.
     */
    fun activeDevice(devices: List<AudioDevice>): AudioDevice? =
        normalize(devices).minByOrNull { priority(it.category) }

    fun priority(category: DeviceCategory): Int = when (category) {
        DeviceCategory.BLUETOOTH -> 0
        DeviceCategory.USB_DAC -> 1
        DeviceCategory.WIRED -> 2
        DeviceCategory.OTHER -> 3
        DeviceCategory.SPEAKER -> 4
    }

    fun makeId(category: DeviceCategory, key: String): String {
        val slug = key.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(64)
            .ifEmpty { "default" }
        return "${category.name}:$slug"
    }

    private fun fallbackName(type: Int, category: DeviceCategory): String =
        typeLabel(type).takeIf { it != "Audio output" } ?: category.displayName
}
