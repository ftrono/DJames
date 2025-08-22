package com.ftrono.DJames.application

import android.Manifest
import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import net.openid.appauth.AuthorizationServiceConfiguration
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.application.App.ObjectBox.store
import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.Contact
import com.ftrono.DJames.be.database.LibraryUtils
import com.ftrono.DJames.be.database.Message
import com.ftrono.DJames.be.database.MessageUtils
import com.ftrono.DJames.be.database.MyObjectBox
import com.ftrono.DJames.be.database.Playlist
import com.ftrono.DJames.be.database.Podcast
import com.ftrono.DJames.be.database.Route
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.FulfillmentUtils
import com.ftrono.DJames.be.nlp.NLPDispatcher
import com.ftrono.DJames.be.samples.DefaultReplies
import com.ftrono.DJames.be.spotify.SpotifyLoginUtils
import com.ftrono.DJames.be.spotify.SpotifyUtils
import com.ftrono.DJames.ui.theme.NavigationItem
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
val appVersion = "3.0.a3 (alpha)"
val copyrightYear = 2024

//DB:
var messageBox: Box<Message>? = null
var artistBox: Box<Artist>? = null
var playlistBox: Box<Playlist>? = null
var podcastBox: Box<Podcast>? = null
var contactBox: Box<Contact>? = null
var routeBox: Box<Route>? = null

//UTILS:
val utils = Utilities()
val libUtils = LibraryUtils()
val messageUtils = MessageUtils()
val spotifyUtils = SpotifyUtils()
val spotifyLoginUtils = SpotifyLoginUtils()
val fulfillmentUtils = FulfillmentUtils()
val defaultReplies = DefaultReplies()

//Navigation:
val navigationItems = listOf(
    NavigationItem.Library,
    NavigationItem.Home,
    NavigationItem.Messages
)

//Permissions:
val runtimePermissions = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)   // Android 13+
    }
}
val permissionDescriptions = buildMap {
    put(Manifest.permission.RECORD_AUDIO, "DJames needs access to your microphone to record audio.\n\nHe will only use it when you click the Overlay button.")
    put(Manifest.permission.CALL_PHONE, "DJames needs the permission to make phone calls. He will only make calls when you ask for it.")
    put(Manifest.permission.READ_PHONE_STATE, "DJames needs the permission to access your phone's state for managing calls & audio features.")
    put(Manifest.permission.SEND_SMS, "DJames needs the permission to send SMS messages. He will only send SMSs when you ask for it.")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        put(Manifest.permission.POST_NOTIFICATIONS, "DJames needs the permission to post notifications.\n\nHe ONLY needs them to inform you when it is active (he won't EVER send you any other notification).")
    }
}
val overlayPermissionDescription = "DJames needs your permission to show its Overlay button over other apps! Through this button, you will be able to record voice requests.\n\nPlease tap 'Yes' and enable DJames from the app list!"

//STATUS VARS:
var curNavId = 0
var lastNavRoute = "home"
var permsRequested = MutableLiveData<Boolean>(false)
var spotifyLoggedIn = MutableLiveData<Boolean>(false)
var spotUserName = MutableLiveData<String>("")
var userGender = MutableLiveData<String>("Sir")
var overlayActive = MutableLiveData<Boolean>(false)
var queryStatus = MutableLiveData<String>("ready")   // MAIN PROCESS STATE FOR BOTH VOICE & CHAT!!!
var clockActive = MutableLiveData<Boolean>(false)
var overlayPos = MutableLiveData<String>("Right")
var volumeUpEnabled = MutableLiveData<Boolean>(true)
var sourceIsVolume = MutableLiveData<Boolean>(false)
var extraOpen = MutableLiveData<Boolean>(false)
var innerNavOpen = MutableLiveData<Boolean>(false)
var currentPlayingPrefix = MutableLiveData<String>("")
var currentSongPlaying = MutableLiveData<String>("Don't turn off the screen!")
var currentArtistPlaying = MutableLiveData<String>("You can keep this Clock\nScreen on to save battery")
var clickCounter = MutableLiveData<Int>(0)
var autoStopQueriesState = MutableLiveData<Boolean>(false)
var allowVolumeClick = true
var userNicknameState = MutableLiveData<String>("")
var spotUserImageState = MutableLiveData<String>("")
var addLinkOn = MutableLiveData<Boolean>(false)
var sharedLink = MutableLiveData<String>("")

//Library & Messages:
var curLibrarySize = MutableLiveData<Int>(0)
val libHeads = listOf("artist", "playlist", "podcast", "contact", "route")
val libSectionIdentifier = "%%%SECTIONSECTIONSECTION%%%"
var allMessageIds = MutableLiveData<List<Long>>(listOf<Long>())

// Conversation tracking:
var chatLastDispatch = DispatcherInfo()
var lastAiMessage: Message = Message()
var lastUserMessage: Message = Message()
var lastRequestIntent: String = ""
var lastStarterId: Long = 0L

//Preferences:
val defaultChatResetTime: Long = 3*60*1000   //minutes
val defaultChatWait = 2000L
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
val datetimeExportFormat = "yyyy-MM-dd HH_mm_ss"
val datetimeFullFormat = "yyyy/MM/dd HH:mm"
val datetimeShortFormat = "MMMM dd, HH:mm"

//Dropdowns:
val genders = listOf<String>("Sir", "Madam")
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
var recordingFail: Boolean = false
var recordingTime = 0

//Audio Managers:
var audioManager: AudioManager? = null

//Player info:
var currently_playing: JsonObject? = null
var nlp_queryText = ""
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
const val ACTION_MESSAGES_REFRESH = "com.ftrono.DJames.eventReceiver.ACTION_MESSAGES_REFRESH"

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
        messageBox = store.boxFor(Message::class.java)
        artistBox = store.boxFor(Artist::class.java)
        playlistBox = store.boxFor(Playlist::class.java)
        podcastBox = store.boxFor(Podcast::class.java)
        contactBox = store.boxFor(Contact::class.java)
        routeBox = store.boxFor(Route::class.java)
    }
}
