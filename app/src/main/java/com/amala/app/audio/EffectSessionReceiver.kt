package com.amala.app.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

/**
 * Music players can announce the audio session they opened so effect-control
 * apps can attach effects to it. This is the fallback for devices that ignore
 * global (session 0) effects.
 */
class EffectSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        if (sessionId == 0) return
        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> AudioController.onSessionOpened(sessionId)
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> AudioController.onSessionClosed(sessionId)
        }
    }
}
