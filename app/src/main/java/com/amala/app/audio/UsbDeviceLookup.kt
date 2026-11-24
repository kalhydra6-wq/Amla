package com.amala.app.audio

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.amala.app.model.AudioDevice
import com.amala.app.model.UsbIdentity

/** Matches an output device to a physical USB device via address, product or manufacturer name. */
object UsbDeviceLookup {

    fun find(context: Context, device: AudioDevice?): UsbIdentity? {
        if (device == null || device.category != com.amala.app.model.DeviceCategory.USB_DAC) return null
        val manager = runCatching {
            context.getSystemService(Context.USB_SERVICE) as UsbManager
        }.getOrNull() ?: return null
        val devices = runCatching { manager.deviceList.values.toList() }.getOrDefault(emptyList())
        if (devices.isEmpty()) return null

        val address = device.address.orEmpty()
        val name = device.name
        val match = devices.firstOrNull { usb ->
            addressMatches(usb, address) || nameMatches(usb, name)
        } ?: return null

        return UsbIdentity(
            manufacturer = runCatching { match.manufacturerName }.getOrNull()?.takeIf { it.isNotBlank() },
            vendorId = match.vendorId,
            productId = match.productId,
        )
    }

    private fun addressMatches(usb: UsbDevice, address: String): Boolean {
        if (address.isBlank()) return false
        val deviceName = runCatching { usb.deviceName }.getOrNull().orEmpty()
        if (deviceName.isBlank()) return false
        if (deviceName.equals(address, ignoreCase = true)) return true
        if (address.contains(deviceName, ignoreCase = true)) return true
        val suffix = deviceName.takeLast(12)
        return suffix.length >= 5 && address.contains(suffix, ignoreCase = true)
    }

    private fun nameMatches(usb: UsbDevice, name: String): Boolean {
        if (name.isBlank()) return false
        val product = runCatching { usb.productName }.getOrNull()?.takeIf { it.length >= 3 }
        val manufacturer = runCatching { usb.manufacturerName }.getOrNull()?.takeIf { it.length >= 3 }
        return listOfNotNull(product, manufacturer).any { part ->
            name.contains(part, ignoreCase = true) || part.contains(name, ignoreCase = true)
        }
    }
}
