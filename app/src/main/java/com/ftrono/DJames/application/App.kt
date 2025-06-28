package com.ftrono.DJames.application

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import android.net.Uri
import net.openid.appauth.AuthorizationServiceConfiguration
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.application.App.ObjectBox.store
import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.Contact
import com.ftrono.DJames.be.database.HistoryLog
import com.ftrono.DJames.be.database.HistoryUtils
import com.ftrono.DJames.be.database.LibraryUtils
import com.ftrono.DJames.be.database.MyObjectBox
import com.ftrono.DJames.be.database.Playlist
import com.ftrono.DJames.be.database.Podcast
import com.ftrono.DJames.be.database.Route
import com.ftrono.DJames.be.nlp.FulfillmentUtils
import com.ftrono.DJames.be.spotify.SpotifyUtils
import com.ftrono.DJames.utilities.Prefs
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import io.objectbox.Box
import io.objectbox.BoxStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


//GLOBALS:
val prefs: Prefs by lazy {
    App.prefs!!
}
val appVersion = "2.5.2"
val copyrightYear = 2024

//DB:
var historyBox: Box<HistoryLog>? = null
var artistBox: Box<Artist>? = null
var playlistBox: Box<Playlist>? = null
var podcastBox: Box<Podcast>? = null
var contactBox: Box<Contact>? = null
var routeBox: Box<Route>? = null

//UTILS:
val utils = Utilities()
val libUtils = LibraryUtils()
val logUtils = HistoryUtils()
val spotifyUtils = SpotifyUtils()
val fulfillmentUtils = FulfillmentUtils()

//STATUS VARS:
var curNavId = 0
var lastNavRoute = "home"
var spotifyLoggedIn = MutableLiveData<Boolean>(false)
var overlayActive = MutableLiveData<Boolean>(false)
var overlayStatus = MutableLiveData<String>("ready")
var clockActive = MutableLiveData<Boolean>(false)
var overlayPos = MutableLiveData<String>("Right")
var volumeUpEnabled = MutableLiveData<Boolean>(true)
var sourceIsVolume = MutableLiveData<Boolean>(false)
var settingsOpen = MutableLiveData<Boolean>(false)
var innerNavOpen = MutableLiveData<Boolean>(false)
var currentPlayingPrefix = MutableLiveData<String>("")
var currentSongPlaying = MutableLiveData<String>("Don't turn off the screen!")
var currentArtistPlaying = MutableLiveData<String>("You can keep this Clock\nScreen on to save battery")
var clickCounter = MutableLiveData<Int>(0)
var autoStopQueriesState = MutableLiveData<Boolean>(false)
var allowVolumeClick = true
var genderMaleState = MutableLiveData<Boolean>(true)
var userNicknameState = MutableLiveData<String>("")
var spotUserImageState = MutableLiveData<String>("")
var addLinkOn = MutableLiveData<Boolean>(false)
var sharedLink = MutableLiveData<String>("")

//Library:
var curLibrarySize = MutableLiveData<Int>(0)
val libHeads = listOf("artist", "playlist", "podcast", "contact", "route")
val libSectionIdentifier = "%%%SECTIONSECTIONSECTION%%%"
var curHistorySize = MutableLiveData<Int>(0)
var historyItems = MutableLiveData<List<String>>(listOf<String>())

//Preferences:
val maxAudioRecTimeout = 120L   //for voice messages
val maxClickOptions = 3
val silenceInitPatience = 6
val silencePatience = 2
val deltaSimilarity = 10   //5
val playThreshold = 80
val maxThreshold = 70
val midThreshold = 60
val recSamplingRate = 44100
val queryTimeout = 5   //seconds
val recFileName = "DJames_request"
var enablePlayerInfo = false

//Dropdowns:
var queryLangCodes = listOf<String>("en", "it")
val queryLangFull = listOf<String>("English", "Italian")
var messLangCodes = listOf<String>("en", "it", "fr", "de", "es")
val messLangFull = listOf<String>("English", "Italian", "French", "German", "Spanish")
var messLangLower = listOf<String>("english", "italian", "french", "german", "spanish")

//Modes:
var density: Float = 0F
var acts_active: MutableList<String> = mutableListOf<String>()
var streamMaxVolume: Int = 0
var screenOn: Boolean = true
var main_initialized: Boolean = false
var vol_initialized: Boolean = false
var voiceQueryOn: Boolean = false
var recordingMode: Boolean = false
var callMode: Boolean = false
var searchFail: Boolean = false

//Audio Managers:
var audioManager: AudioManager? = null

//JSONs:
var currently_playing: JsonObject? = null
var lastLog: HistoryLog = HistoryLog()

//Player info:
var nlp_queryText = ""
var reqPlayLinkName = ""
var currentTrackId: String = ""
var songName: String = ""
var artistName: String = ""
var contextName: String = ""

//HTTP:
val gMapsLinkFormat = "https://www.google.com/maps/dir//"

//Spotify formats:
val spotIntroUri = "spotify"   // spotify:<type>:<id>
val spotIntroUrl = "https://open.spotify.com"   // .../<type>/<id>
val trackUrlIntro = "https://open.spotify.com/track/"
val artistUrlIntro = "https://open.spotify.com/artist/"
val playlistUrlIntro = "https://open.spotify.com/playlist/"
val showUrlIntro = "https://open.spotify.com/show/"
val episodeUrlIntro = "https://open.spotify.com/episode/"
val likedSongsUri = "spotify:user:replaceUserId:collection"

//Spotify:
val spotifyQueryLimit = 10
var spotTempToken = ""
var refrTempToken = ""
var showLoggingIn = MutableLiveData<Boolean>(false)
val redirectUri = "djames-oauth://callback"   //URLEncoder.encode(redirectUriOrig, "UTF-8")
val spotifyAuthConfig = AuthorizationServiceConfiguration(
    Uri.parse("https://accounts.spotify.com/authorize"), // Authorization endpoint
    Uri.parse("https://accounts.spotify.com/api/token")   // Token endpoint
)
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
const val SPOTIFY_METADATA_CHANGED = "com.spotify.music.metadatachanged"
const val ACTION_TOASTER = "com.ftrono.DJames.eventReceiver.ACTION_TOASTER"

//Main Act receiver:
const val ACTION_FINISH_MAIN = "com.ftrono.DJames.eventReceiver.ACTION_FINISH_MAIN"
const val ACTION_LOG_REFRESH = "com.ftrono.DJames.eventReceiver.ACTION_LOG_REFRESH"

//Clock Act receiver:
const val ACTION_TIME_TICK = "android.intent.action.TIME_TICK"
const val ACTION_UPDATE_PLAYER = "com.ftrono.DJames.eventReceiver.ACTION_UPDATE_PLAYER"
const val ACTION_FINISH_CLOCK = "com.ftrono.DJames.eventReceiver.ACTION_FINISH_CLOCK"

//Overlay receiver:
const val ACTION_OVERLAY_CLICK = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_CLICK"
const val ACTION_SAVE_TRACK = "com.ftrono.DJames.eventReceiver.ACTION_SAVE_TRACK"
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

    object ObjectBox {
        lateinit var store: BoxStore
            private set

        fun init(context: Context) {
            store = MyObjectBox.builder()
                .androidContext(context)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        prefs = Prefs(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        streamMaxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        //DB:
        ObjectBox.init(this)
        historyBox = store.boxFor(HistoryLog::class.java)
        artistBox = store.boxFor(Artist::class.java)
        playlistBox = store.boxFor(Playlist::class.java)
        podcastBox = store.boxFor(Podcast::class.java)
        contactBox = store.boxFor(Contact::class.java)
        routeBox = store.boxFor(Route::class.java)
    }
}