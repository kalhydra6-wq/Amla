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
        val preferences = AmlaPreferences(SharedPreferencesStore(this))
        preferences.migrateBackgroundOnce()
        preferences.migrateThemeOnce()
        com.amala.app.data.SoundEventRepository.initialize(this)
        AudioController.initialize(this)
        if (preferences.backgroundMode != com.amala.app.model.BackgroundMode.MANUAL) {
            AmlaAudioService.start(this)
        }
    }
}
