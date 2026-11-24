package com.amala.app.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.amala.app.MainActivity
import com.amala.app.R
import com.amala.app.data.AmlaPreferences
import com.amala.app.data.SharedPreferencesStore
import com.amala.app.ui.AmlaTileService
import com.amala.app.ui.AmlaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Keeps the equalizer applied and exposes media-style controls in the foreground notification. */
class AmlaAudioService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val deviceHandler = Handler(Looper.getMainLooper())
    private var ignoreFirstDeviceEvent = true
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = onDeviceChange()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = onDeviceChange()
    }

    private fun onDeviceChange() {
        if (ignoreFirstDeviceEvent) {
            ignoreFirstDeviceEvent = false
            return
        }
        val prefs = AmlaPreferences(SharedPreferencesStore(applicationContext))
        SoftStartController.onOutputDeviceChanged(
            applicationContext,
            prefs.softStartEnabled,
            prefs.softStartSeconds,
        )
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat()
        runCatching { audioManager.registerAudioDeviceCallback(deviceCallback, deviceHandler) }
        scope.launch {
            while (true) {
                delay(REAPPLY_INTERVAL_MS)
                val status = AudioController.status()
                if (!status.hasControl || !status.enabled) {
                    AudioController.reinitialize()
                } else {
                    AudioController.refresh()
                }
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> AudioController.toggleEnabled()
            ACTION_PREV -> AudioController.cyclePreset(-1)
            ACTION_NEXT -> AudioController.cyclePreset(1)
            ACTION_BASS_UP -> AudioController.adjustBass(100)
            ACTION_BASS_DOWN -> AudioController.adjustBass(-100)
        }
        if (intent?.action != null) {
            updateNotification()
            runCatching { AmlaWidget.update(this) }
            AmlaTileService.requestUpdate(this)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        SoftStartController.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, AmlaAudioService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun buildNotification(): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val on = AudioController.enabled
        val preset = AudioController.profile.preset.label
        val bass = AudioController.profile.bassBoost / 10
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_amla)
            .setContentTitle("Amla · ${if (on) "EQ on" else "EQ off"}")
            .setContentText("Preset: $preset · Bass $bass%")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_action_eq, if (on) "EQ off" else "EQ on", actionIntent(ACTION_TOGGLE, 1))
            .addAction(R.drawable.ic_action_prev, "Previous", actionIntent(ACTION_PREV, 2))
            .addAction(R.drawable.ic_action_next, "Next", actionIntent(ACTION_NEXT, 3))
            .addAction(R.drawable.ic_action_bass_down, "Bass -", actionIntent(ACTION_BASS_DOWN, 4))
            .addAction(R.drawable.ic_action_bass_up, "Bass +", actionIntent(ACTION_BASS_UP, 5))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        runCatching {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    companion object {
        private const val CHANNEL_ID = "amla_audio"
        private const val NOTIFICATION_ID = 1001
        private const val REAPPLY_INTERVAL_MS = 5_000L
        private const val ACTION_STOP = "com.amala.app.STOP_SERVICE"
        private const val ACTION_TOGGLE = "com.amala.app.NOTIF_TOGGLE"
        private const val ACTION_PREV = "com.amala.app.NOTIF_PREV"
        private const val ACTION_NEXT = "com.amala.app.NOTIF_NEXT"
        private const val ACTION_BASS_UP = "com.amala.app.NOTIF_BASS_UP"
        private const val ACTION_BASS_DOWN = "com.amala.app.NOTIF_BASS_DOWN"

        fun start(context: Context) {
            val intent = Intent(context, AmlaAudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AmlaAudioService::class.java))
        }
    }
}
