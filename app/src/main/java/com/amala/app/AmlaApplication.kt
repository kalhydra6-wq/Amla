package com.amala.app

import android.app.Application
import com.amala.app.audio.AudioController

class AmlaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AudioController.initialize(this)
    }
}
