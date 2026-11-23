package com.amala.app.audio

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import com.amala.app.model.NowPlayingInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared now-playing state fed by the notification listener and UI refresh. */
object NowPlayingRepository {

    private val _state = MutableStateFlow(NowPlayingInfo())
    val state: StateFlow<NowPlayingInfo> = _state.asStateFlow()

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabled = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            )
        }.getOrNull() ?: return false
        return enabled.split(':').any { component ->
            component.startsWith(context.packageName) && component.contains("/")
        }
    }

    fun refresh(context: Context) {
        val granted = isNotificationAccessGranted(context)
        if (!granted) {
            _state.value = NowPlayingInfo(permissionGranted = false)
            return
        }
        val manager = runCatching {
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        }.getOrNull()
        val sessions = manager?.let { runCatching { it.getActiveSessions(null) }.getOrDefault(emptyList()) }
            ?: emptyList()
        val controller = sessions.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: sessions.firstOrNull()
        update(controller, context)
    }

    fun update(controller: MediaController?, context: Context) {
        val granted = isNotificationAccessGranted(context)
        if (controller == null) {
            _state.value = NowPlayingInfo(permissionGranted = granted)
            return
        }
        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.takeIf { it.isNotBlank() }
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.takeIf { it.isNotBlank() }
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val app = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(controller.packageName, 0)).toString()
        }.getOrNull() ?: controller.packageName

        _state.value = NowPlayingInfo(
            title = title,
            artist = artist,
            album = album,
            sourceApp = app,
            artwork = artwork,
            permissionGranted = granted,
            hasSession = true,
        )
    }
}
