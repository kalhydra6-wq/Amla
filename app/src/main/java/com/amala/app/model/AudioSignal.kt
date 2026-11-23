package com.amala.app.model

import android.media.AudioFormat
import java.util.Locale

/** A track reported by an active media session. Unknown values stay empty (UI shows N/A). */
data class NowPlayingInfo(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val sourceApp: String? = null,
    val artwork: Any? = null,
    val permissionGranted: Boolean = false,
    val hasSession: Boolean = false,
)

/** USB identity matched from UsbManager, or null when no match is available. */
data class UsbIdentity(
    val manufacturer: String?,
    val vendorId: Int,
    val productId: Int,
)

/** Pure formatting helpers — Android-format values in, display strings out. */
object AudioSignal {

    const val NA = "N/A"

    fun formatRate(rateHz: Int?): String =
        if (rateHz == null || rateHz <= 0) NA else "${rateHz / 1000}.${(rateHz % 1000) / 100} kHz"

    fun formatRateCompact(rateHz: Int?): String =
        if (rateHz == null || rateHz <= 0) NA else "${rateHz / 1000}k"

    /** Android reports >=24-bit precision as ENCODING_PCM_FLOAT. */
    fun bitDepth(encoding: Int?): Int? = when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT -> 8
        AudioFormat.ENCODING_PCM_16BIT -> 16
        AudioFormat.ENCODING_PCM_FLOAT -> 24
        else -> null
    }

    fun formatDepth(encoding: Int?): String = bitDepth(encoding)?.let { "$it-bit" } ?: NA

    fun codecName(encoding: Int?): String = when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT,
        AudioFormat.ENCODING_PCM_16BIT,
        AudioFormat.ENCODING_PCM_FLOAT -> "PCM"
        AudioFormat.ENCODING_AC3 -> "Dolby Digital"
        AudioFormat.ENCODING_E_AC3 -> "Dolby Digital+"
        AudioFormat.ENCODING_DTS -> "DTS"
        AudioFormat.ENCODING_DTS_HD -> "DTS-HD"
        AudioFormat.ENCODING_MP3 -> "MP3"
        AudioFormat.ENCODING_AAC_LC -> "AAC-LC"
        AudioFormat.ENCODING_AAC_HE_V1,
        AudioFormat.ENCODING_AAC_HE_V2 -> "HE-AAC"
        else -> NA
    }

    fun formatChannels(count: Int?): String = when {
        count == null || count <= 0 -> NA
        count == 1 -> "Mono"
        count == 2 -> "Stereo"
        else -> "$count ch"
    }

    fun isHiRes(rateHz: Int?, encoding: Int?): Boolean {
        val depth = bitDepth(encoding) ?: return false
        return (rateHz ?: 0) >= 96_000 && depth >= 24
    }

    fun isResampled(inputRate: Int?, outputRate: Int?): Boolean =
        inputRate != null && outputRate != null && inputRate > 0 && outputRate > 0 && inputRate != outputRate

    fun formatVendorId(id: Int): String = String.format(Locale.US, "0x%04X", id and 0xFFFF)

    fun formatBitrate(bitrate: Int?): String =
        if (bitrate == null || bitrate <= 0) NA else "${bitrate / 1000} kbps"
}
