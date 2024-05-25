package com.ftrono.DJames.application

import android.app.Application
import android.content.Context
import android.media.AudioManager
import com.ftrono.DJames.utilities.Prefs
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.io.File

//GLOBALS:
val prefs: Prefs by lazy {
    App.prefs!!
}
val appVersion = "1.11"

//Preferences:
val deltaSimilarity = 0   //30
val maxThreshold = 70
val midThreshold = 55
val matchDoubleThreshold = 160
val recSamplingRate = 44100
val queryTimeout = 5   //seconds
val recFileName = "DJames_request"
var enablePlayerInfo = false
var supportedLanguageCodes = listOf<String>("en-US", "it")

//Modes:
var density: Float = 0F
var acts_active: MutableList<String> = ArrayList()
var streamMaxVolume: Int = 0
var screenOn: Boolean = true
var sourceIsVolume: Boolean = false
var main_initialized: Boolean = false
var vol_initialized: Boolean = false
var voiceSearchOn: Boolean = false
var recordingMode: Boolean = false
var callMode: Boolean = false
var overlay_active: Boolean = false
var loggedIn: Boolean = false
var clock_active: Boolean = false
var searchFail: Boolean = false
var filter = "artist"

//Audio Manager:
var audioManager: AudioManager? = null

//JSONs:
var currently_playing: JsonObject? = null
var last_log: JsonObject? = null
var logDir: File? = null
var vocDir: File? = null

//Player info:
var nlp_queryText = ""
var songName: String = ""
var artistName: String = ""
var contextName: String = ""

//HTTP:
//DialogFlow & Spotify formats:
val dialogflow_id = "djames-nlp"
val uri_format = "spotify:track:"
val ext_format = "http://open.spotify.com/track/"
val playlistUrlIntro = "https://open.spotify.com/playlist/"
val likedSongsUri = "spotify:user:replaceUserId:collection"

//Spotify:
var grantToken = ""
val clientId = "f525169dff664aa192ab51d2bbeb9767"
val clientSct = "c7296c5536b8409297760a7eafa0448a"
val redirectUriOrig ="http://localhost:8888/callback"
val redirectUri = URLEncoder.encode(redirectUriOrig, "UTF-8")
val client = OkHttpClient.Builder()
    .connectTimeout(queryTimeout.toLong(), TimeUnit.SECONDS)
    .writeTimeout(queryTimeout.toLong(), TimeUnit.SECONDS)
    .readTimeout(queryTimeout.toLong(), TimeUnit.SECONDS)
    .callTimeout(queryTimeout.toLong(), TimeUnit.SECONDS)
    .build()

//BROADCASTS:
//Event receiver:
//ACTION_SCREEN_ON, ACTION_SCREEN_OFF
const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
const val ACTION_TOASTER = "com.ftrono.DJames.eventReceiver.ACTION_TOASTER"
const val ACTION_REDIRECT = "com.ftrono.DJames.eventReceiver.ACTION_REDIRECT"

//Main Act receiver:
const val ACTION_MAIN_LOGGED_IN = "com.ftrono.DJames.eventReceiver.ACTION_MAIN_LOGGED_IN"
const val ACTION_FINISH_MAIN = "com.ftrono.DJames.eventReceiver.ACTION_FINISH_MAIN"

//Home Fragment receiver:
const val ACTION_HOME_LOGGED_IN = "com.ftrono.DJames.eventReceiver.ACTION_HOME_LOGGED_IN"
const val ACTION_HOME_LOGGED_OUT = "com.ftrono.DJames.eventReceiver.ACTION_HOME_LOGGED_OUT"
const val ACTION_OVERLAY_ACTIVATED = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_ACTIVATED"
const val ACTION_OVERLAY_DEACTIVATED = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_DEACTIVATED"
const val ACTION_SETTINGS_VOL_UP = "com.ftrono.DJames.eventReceiver.ACTION_SETTINGS_VOL_UP"

//Guide Fragment receiver:
const val ACTION_GUIDE_POPUP = "com.ftrono.DJames.eventReceiver.ACTION_GUIDE_POPUP"
const val ACTION_GUIDE_REFRESH = "com.ftrono.DJames.eventReceiver.ACTION_GUIDE_REFRESH"

//Vocabulary Fragment receiver:
const val ACTION_VOC_DELETE = "com.ftrono.DJames.eventReceiver.ACTION_VOC_DELETE"
const val ACTION_VOC_EDIT = "com.ftrono.DJames.eventReceiver.ACTION_VOC_EDIT"

//History Fragment receiver:
const val ACTION_LOG_REFRESH = "com.ftrono.DJames.eventReceiver.ACTION_LOG_REFRESH"
const val ACTION_LOG_DELETE = "com.ftrono.DJames.eventReceiver.ACTION_LOG_DELETE"

//Settings Act receiver:
const val ACTION_SETTINGS_LOGGED_IN = "com.ftrono.DJames.eventReceiver.ACTION_SETTINGS_LOGGED_IN"
const val ACTION_FINISH_SETTINGS = "com.ftrono.DJames.eventReceiver.ACTION_FINISH_SETTINGS"

//Clock Act receiver:
const val ACTION_TIME_TICK = "android.intent.action.TIME_TICK"
const val SPOTIFY_METADATA_CHANGED = "com.spotify.music.metadatachanged"
const val ACTION_FINISH_CLOCK = "com.ftrono.DJames.eventReceiver.ACTION_FINISH_CLOCK"

//Overlay receiver:
const val ACTION_CLOCK_OPENED = "com.ftrono.DJames.eventReceiver.ACTION_CLOCK_OPENED"
const val ACTION_CLOCK_CLOSED = "com.ftrono.DJames.eventReceiver.ACTION_CLOCK_CLOSED"
const val ACTION_OVERLAY_READY = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_READY"
const val ACTION_OVERLAY_BUSY = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_BUSY"
const val ACTION_OVERLAY_PROCESSING = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_PROCESSING"
const val ACTION_MAKE_CALL = "com.ftrono.DJames.eventReceiver.ACTION_MAKE_CALL"
const val PHONE_STATE_ACTION = "android.intent.action.PHONE_STATE"

//Voice Search receiver:
const val ACTION_REC_EARLY_STOP = "com.ftrono.DJames.eventReceiver.ACTION_REC_EARLY_STOP"


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