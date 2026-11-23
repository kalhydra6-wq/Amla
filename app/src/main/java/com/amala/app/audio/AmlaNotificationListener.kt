package com.amala.app.audio

import android.media.session.MediaController
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/** Receives now-playing updates from media sessions once notification access is granted. */
class AmlaNotificationListener : NotificationListenerService() {

    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        super.onActiveSessionsChanged(controllers)
        val active = controllers.orEmpty()
        callbacks.forEach { (controller, callback) ->
            if (controller !in active) runCatching { controller.unregisterCallback(callback) }
        }
        callbacks.entries.removeAll { (controller, _) -> controller !in active }

        active.forEach { controller ->
            if (callbacks.containsKey(controller)) return@forEach
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                    NowPlayingRepository.update(controller, applicationContext)
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    NowPlayingRepository.update(controller, applicationContext)
                }
            }
            runCatching { controller.registerCallback(callback) }
            callbacks[controller] = callback
        }

        val playing = active.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: active.firstOrNull()
        NowPlayingRepository.update(playing, applicationContext)
    }

    override fun onDestroy() {
        callbacks.forEach { (controller, callback) ->
            runCatching { controller.unregisterCallback(callback) }
        }
        callbacks.clear()
        super.onDestroy()
    }
}
