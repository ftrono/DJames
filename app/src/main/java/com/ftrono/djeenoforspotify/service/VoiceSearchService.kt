package com.ftrono.djeenoforspotify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.application.prefs
import com.ftrono.djeenoforspotify.recorder.AndroidAudioRecorder
import java.io.File


class VoiceSearchService : Service() {
    //Main:
    private val TAG = VoiceSearchService::class.java.simpleName
    private var listening: Boolean = false
    private val saveDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    //Recorder:
    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //Audio manager:
    private var audioManager: AudioManager? = null
    private var audioAttributes: AudioAttributes? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var focusState: Boolean = false
    private var mAudioFocusPlaybackDelayed: Boolean = false
    private var mAudioFocusResumeOnFocusGained: Boolean = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusState = true
                Log.d(TAG, "Audio focus gained!")
                if (mAudioFocusPlaybackDelayed || mAudioFocusResumeOnFocusGained) {
                    mAudioFocusPlaybackDelayed = false
                    mAudioFocusResumeOnFocusGained = false
                }
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
                focusState = true
                Log.d(TAG, "Audio focus transient exclusive gained!")
                if (mAudioFocusPlaybackDelayed || mAudioFocusResumeOnFocusGained) {
                    mAudioFocusPlaybackDelayed = false
                    mAudioFocusResumeOnFocusGained = false
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                focusState = false
                Log.d(TAG, "Audio focus lost.")
                mAudioFocusResumeOnFocusGained = false
                mAudioFocusPlaybackDelayed = false
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                focusState = false
                Log.d(TAG, "Audio focus transient lost.")
                mAudioFocusResumeOnFocusGained = true
                mAudioFocusPlaybackDelayed = false
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()

            //Audio manager:
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            //Focus request:
            audioFocusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(audioAttributes!!)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

            //REQUEST AUDIO FOCUS:
            val focusRequest = audioManager!!.requestAudioFocus(audioFocusRequest!!)
            when (focusRequest) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    Toast.makeText(
                        applicationContext,
                        "Cannot gain audio focus! Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    //START VOICE SEARCH:
                    countdownStart()
                }

                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    mAudioFocusPlaybackDelayed = true;
                }
            }

        } catch (e: Exception) {
            Log.d(TAG, "Exception: ", e)
            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent1.putExtra("fromwhere", "ser")
            intent1.setData(uri)
            startActivity(intent1)
            stopSelf()
        }
    }

    private fun startForeground() {
        //Foreground service:
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.djeenoForSpotify"
        val channelName = "Voice Search Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)!!
        manager.createNotificationChannel(chan)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("Djeeno: Floating View Service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(3, notification)
    }

    fun countdownStart() {

        //prepare thread:
        val mThread = Thread {
            try {
                synchronized(this) {
                    //RECORDING:
                    //Start recording (default: cacheDir):
                    var it = File(saveDir, "audio.mp3")
                    recorder.start(it)
                    //Countdown:
                    Thread.sleep(prefs.recTimeout.toLong() * 1000)   //default: 5000
                    //Stop recording:
                    recorder.stop()
                    //Abandon audio focus:
                    //if (!focusState) {}
                    audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)

                    //AFTER RECORDING:
                    //Spotify result:
                    var spotifyToOpen =
                        "https://open.spotify.com/track/3jFP1e8IUpD9QbltEI1Hcg?si=pt790-QFRyWr2JhyoMb_yA"
                    //Open links and redirects:
                    openResults(spotifyToOpen)
                    listening = false
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
            }
        }
        //start thread:
        mThread.start()
    }

    private fun openResults(spotifyToOpen: String) {

        //Open query result in Spotify:
        val intentSpotify = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(spotifyToOpen)
        )
        intentSpotify.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentSpotify.putExtra("fromwhere", "ser")
        startActivity(intentSpotify)

        //Maps redirect:
        if (prefs.navEnabled) {
            Thread.sleep(prefs.mapsTimeout.toLong() * 1000)   //default: 3000
            //Launch Maps:
            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(prefs.mapsAddress)
            )
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mapIntent.putExtra("fromwhere", "ser")
            startActivity(mapIntent)
        }

        //Stop Voice Search service:
        stopSelf()
    }

}
