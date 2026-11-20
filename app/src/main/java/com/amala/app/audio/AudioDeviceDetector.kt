package com.amala.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.amala.app.model.AudioDevice
import com.amala.app.model.AudioDevices

/**
 * Watches Android's audio routing in real time: Bluetooth (including LE Audio),
 * USB DACs, wired headsets and the built-in speaker. Every change is delivered
 * as a normalized, prioritized device list.
 */
class AudioDeviceDetector(
    private val context: Context,
    private val onChanged: (List<AudioDevice>) -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var latest: List<AudioDevice> = emptyList()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    fun start() {
        if (started) return
        started = true
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        runCatching {
            context.registerReceiver(headsetReceiver, IntentFilter(AudioManager.ACTION_HEADSET_PLUG))
        }
        refresh()
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        runCatching { context.unregisterReceiver(headsetReceiver) }
    }

    fun refresh() {
        latest = queryDevices()
        onChanged(latest)
    }

    fun currentDevices(): List<AudioDevice> = latest

    fun queryDevices(): List<AudioDevice> {
        val infos = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val devices = infos.mapNotNull { info -> AudioDevices.fromInfo(info) }
        return AudioDevices.normalize(devices)
    }
}
