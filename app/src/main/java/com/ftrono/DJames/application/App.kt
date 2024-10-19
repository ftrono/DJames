package com.ftrono.DJames.application

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.utilities.Prefs
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.io.File

//GLOBALS:
val prefs: Prefs by lazy {
    App.prefs!!
}
val appVersion = "2.1"
val copyrightYear = 2024

//STATUS VARS:
var curNavId = 0
var lastNavRoute = "home"
var spotifyLoggedIn = MutableLiveData<Boolean>(false)
var mainSubtitle = MutableLiveData<String>("")
var overlayActive = MutableLiveData<Boolean>(false)
var volumeUpEnabled = MutableLiveData<Boolean>(true)
var settingsOpen = MutableLiveData<Boolean>(false)
var innerNavOpen = MutableLiveData<Boolean>(false)
var filter = MutableLiveData<String>("artist")
var currentSongPlaying = MutableLiveData<String>("Don't turn off the screen!")
var currentArtistPlaying = MutableLiveData<String>("Keep this Clock Screen on\nto save battery")
var currentAlbumPlaying = MutableLiveData<String>("(unless you're using Maps)")
var historySize = MutableLiveData<Int>(0)

//Vocabulary sections (must match in length):
val headOrder = listOf("artist", "playlist", "contact")
val headStates = listOf(true, false, false)

//Preferences:
val silenceInitPatience = 3
val silencePatience = 2
val deltaSimilarity = 10   //5
val playThreshold = 80
val maxThreshold = 70
val midThreshold = 55
val recSamplingRate = 44100
val queryTimeout = 5   //seconds
val recFileName = "DJames_request"
var enablePlayerInfo = false

//Dropdowns:
var overlayPosOptions = listOf<String>("Left", "Right")
var queryLangCodes = listOf<String>("en", "it")
val queryLangCaps = listOf<String>("English", "Italian")
var messLangCodes = listOf<String>("en", "it", "fr", "de", "es")
var messLangNames = listOf<String>("english", "italian", "french", "german", "spanish")
val messLangCaps = listOf<String>("English", "Italian", "French", "German", "Spanish")

//Modes:
var density: Float = 0F
var acts_active: MutableList<String> = ArrayList()
var streamMaxVolume: Int = 0
var screenOn: Boolean = true
var sourceIsVolume: Boolean = false
var main_initialized: Boolean = false
var vol_initialized: Boolean = false
var voiceQueryOn: Boolean = false
var recordingMode: Boolean = false
var callMode: Boolean = false
var clock_active: Boolean = false
var searchFail: Boolean = false
var newsTalk = false

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
//Spotify formats:
val uri_format = "spotify:track:"   ///spotify:<type>:<id>
val ext_format = "http://open.spotify.com/"
val playlistUrlIntro = "https://open.spotify.com/playlist/"
val likedSongsUri = "spotify:user:replaceUserId:collection"

//Spotify:
var grantToken = ""
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

//Main Act receiver:
const val ACTION_MAIN_LOGGED_IN = "com.ftrono.DJames.eventReceiver.ACTION_MAIN_LOGGED_IN"
const val ACTION_FINISH_MAIN = "com.ftrono.DJames.eventReceiver.ACTION_FINISH_MAIN"
const val ACTION_OVERLAY_ACTIVATED = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_ACTIVATED"
const val ACTION_OVERLAY_DEACTIVATED = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_DEACTIVATED"
const val ACTION_LOG_REFRESH = "com.ftrono.DJames.eventReceiver.ACTION_LOG_REFRESH"

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

//Voice Query receiver:
const val ACTION_REC_STOP = "com.ftrono.DJames.eventReceiver.ACTION_REC_STOP"

//AUDIOFOCUS:
//AudioFocus:
var focusState: Boolean = false
var audioFocusRequest: AudioFocusRequest? = null

//var mAudioFocusPlaybackDelayed: Boolean = false
//var mAudioFocusResumeOnFocusGained: Boolean = false

var audioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
        AudioManager.AUDIOFOCUS_GAIN -> {
            focusState = true
            Log.d("DJames", "Audio focus gained!")
            /*
            if (mAudioFocusPlaybackDelayed || mAudioFocusResumeOnFocusGained) {
                mAudioFocusPlaybackDelayed = false
                mAudioFocusResumeOnFocusGained = false
            }
            */
        }

        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
            focusState = true
            Log.d("DJames", "Audio focus transient exclusive gained!")
            /*
            if (mAudioFocusPlaybackDelayed || mAudioFocusResumeOnFocusGained) {
                mAudioFocusPlaybackDelayed = false
                mAudioFocusResumeOnFocusGained = false
            }
            */
        }

        AudioManager.AUDIOFOCUS_LOSS -> {
            focusState = false
            Log.d("DJames", "Audio focus lost.")
            // mAudioFocusResumeOnFocusGained = false
            // mAudioFocusPlaybackDelayed = false
        }

        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
            focusState = false
            Log.d("DJames", "Audio focus transient lost.")
            // mAudioFocusResumeOnFocusGained = false
            // mAudioFocusPlaybackDelayed = false
        }
    }
}


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