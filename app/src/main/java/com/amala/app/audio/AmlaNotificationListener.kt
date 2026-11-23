package com.amala.app.audio

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService

/** Receives now-playing updates from media sessions once notification access is granted. */
class AmlaNotificationListener : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        onSessionsChanged(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val manager = runCatching {
            getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        }.getOrNull() ?: return
        runCatching {
            manager.addOnActiveSessionsChangedListener(
                sessionListener,
                ComponentName(this, AmlaNotificationListener::class.java),
                handler,
            )
        }
        onSessionsChanged(
            runCatching {
                manager.getActiveSessions(ComponentName(this, AmlaNotificationListener::class.java))
            }.getOrDefault(emptyList()),
        )
    }

    override fun onListenerDisconnected() {
        val manager = runCatching {
            getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        }.getOrNull()
        manager?.let { runCatching { it.removeOnActiveSessionsChangedListener(sessionListener) } }
        super.onListenerDisconnected()
    }

    private fun onSessionsChanged(controllers: List<MediaController>) {
        callbacks.entries.removeAll { (controller, _) -> controller !in controllers }
        controllers.forEach { controller ->
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
        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()
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
