package com.ftrono.djeenoforspotify.application

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.widget.ImageView
import android.widget.RelativeLayout
import com.ftrono.djeenoforspotify.utilities.Prefs

//GLOBALS:
val prefs: Prefs by lazy {
    App.prefs!!
}
var powerManager: PowerManager?= null
var audioManager: AudioManager? = null
var streamMaxVolume : Int = 0
var screenOn : Boolean = true
var recordingMode: Boolean = false

//Overlay resources:
var overlayButton: RelativeLayout? = null
var overlayIcon: ImageView? = null


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
        streamMaxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    }
}