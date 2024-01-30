package com.ftrono.DJames.application

import androidx.appcompat.app.ActionBar
import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.view.MenuItem
import android.widget.ImageView
import android.view.View
import android.widget.TextView
import android.widget.Button
import com.ftrono.DJames.utilities.Prefs
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

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
var loggedIn: Boolean = false

//Spotify Scopes:
val scopes = arrayOf(
    "user-read-private",   //Read access to user’s email address
    "user-read-email",   //Read access to user’s subscription details (type of user account)
    "user-read-playback-state",   //Read access to a user’s player state (devices, player state, track)
    "user-modify-playback-state",   //Write access to a user’s playback state (add to queue)
    "user-read-currently-playing",   //Read your currently playing content (track / queue)
    "app-remote-control",   //Android only: communicate with the Spotify app on your device
    "streaming",   //Play content and control playback on your other devices
    "playlist-read-private",   //Read access to user's private playlists
    "playlist-read-collaborative",   //Include collaborative playlists when requesting a user's playlists
    "playlist-modify-private",   //Write access to a user's private playlists
    "playlist-modify-public",   //Write access to a user's public playlists
    "user-follow-modify",   //Write/delete access to the list of artists and other users that the user follows
    "user-follow-read",   //Read access to the list of artists and other users that the user follows
    "user-top-read",   //Read access to a user's top artists and tracks
    "user-read-recently-played",   //Read access to a user’s recently played tracks
    "user-library-read",   //Access saved content (tracks, albums)
    "user-library-modify"   //Manage saved content (tracks, albums)
)

val scope = scopes.joinToString("%20", "", "")

//HTTP:
var grantToken = ""
val clientId = "f525169dff664aa192ab51d2bbeb9767"
val clientSct = "c7296c5536b8409297760a7eafa0448a"
val redirectUriOrig ="http://localhost:8888/callback"
val redirectUri = URLEncoder.encode(redirectUriOrig, "UTF-8")
val client = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .writeTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .callTimeout(5, TimeUnit.SECONDS)
    .build()

//Overlay resources:
var overlayButton: View? = null
var overlayIcon: ImageView? = null

//View resources:
var mainActionBar: ActionBar? = null
var descr_login_status: TextView? = null
var descr_main: TextView? = null
var descr_use: TextView? = null
var face_cover: View? = null
var startButton: Button? = null
var loginButton: MenuItem? = null

//Broadcasts:
const val ACTION_LOGGED_IN = "com.ftrono.DJames.eventReceiver.ACTION_LOGGED_IN"
const val ACTION_OVERLAY_DEACTIVATED = "com.ftrono.DJames.eventReceiver.ACTION_OVERLAY_DEACTIVATED"


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