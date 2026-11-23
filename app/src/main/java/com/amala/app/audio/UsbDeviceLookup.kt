package com.amala.app.audio

import android.content.Context
import android.hardware.usb.UsbManager
import com.amala.app.model.AudioDevice
import com.amala.app.model.UsbIdentity

/** Matches an audio output device address to a physical USB device. */
object UsbDeviceLookup {

    fun find(context: Context, device: AudioDevice?): UsbIdentity? {
        if (device == null || device.address.isNullOrBlank()) return null
        val manager = runCatching {
            context.getSystemService(Context.USB_SERVICE) as UsbManager
        }.getOrNull() ?: return null
        val devices = runCatching { manager.deviceList.values.toList() }.getOrDefault(emptyList())
        if (devices.isEmpty()) return null

        val address = device.address.orEmpty()
        val match = devices.firstOrNull { usb ->
            val name = runCatching { usb.deviceName }.getOrNull().orEmpty()
            name == address || address.contains(name, ignoreCase = true) ||
                name.contains(address.takeLast(12), ignoreCase = true)
        } ?: return null

        return UsbIdentity(
            manufacturer = runCatching { match.manufacturerName }.getOrNull()?.takeIf { it.isNotBlank() },
            vendorId = match.vendorId,
            productId = match.productId,
        )
    }
}
