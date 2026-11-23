package com.amala.app

import android.app.Application
import com.amala.app.audio.AmlaAudioService
import com.amala.app.audio.AudioController
import com.amala.app.data.AmlaPreferences
import com.amala.app.data.SharedPreferencesStore

class AmlaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        AudioController.initialize(this)
        if (AmlaPreferences(SharedPreferencesStore(this)).keepAlive) {
            AmlaAudioService.start(this)
        }
    }
}
