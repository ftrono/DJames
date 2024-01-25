package com.ftrono.DJames.application

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.widget.ImageView
import android.view.View
import android.widget.TextView
import com.ftrono.DJames.utilities.Prefs

//GLOBALS:
val prefs: Prefs by lazy {
    App.prefs!!
}
var powerManager: PowerManager?= null
var audioManager: AudioManager? = null
var streamMaxVolume : Int = 0
var screenOn : Boolean = true
var recordingMode: Boolean = false
var overlay_active: Boolean = false

//Overlay resources:
var overlayButton: View? = null
var overlayIcon: ImageView? = null

//View resources:
var mapsView: View? = null
var mapsTitle: TextView? = null
var mapsDescr: TextView? = null
var clockView: View? = null
var clockTitle: TextView? = null
var clockDescr: TextView? = null

//Broadcasts:
const val ACTION_OVERLAY_DEACTIVATED = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_DEACTIVATED"
const val ACTION_MODE_CHANGED = "com.ftrono.DJames.eventReceiver.ACTION_MODE_CHANGED"


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