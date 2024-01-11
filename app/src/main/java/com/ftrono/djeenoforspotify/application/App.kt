package com.ftrono.djeenoforspotify.application

import com.ftrono.djeenoforspotify.utilities.Prefs
import android.app.Application
import android.media.AudioManager
import android.content.Context

val prefs: Prefs by lazy {
    App.prefs!!
}

var audioManager: AudioManager? = null

class App: Application()
{
    companion object {
        var prefs: Prefs? = null
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        prefs = Prefs(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}