package com.ftrono.DJames.service

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
import android.media.ToneGenerator
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.application.*
import com.ftrono.DJames.R
import com.ftrono.DJames.application.FakeLockScreen
import com.ftrono.DJames.recorder.AndroidAudioRecorder
import java.io.File


class VoiceSearchService : Service() {
    //Main:
    private val TAG = VoiceSearchService::class.java.simpleName
    //private val saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    //Recorder:
    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //Audio manager:
    private var audioAttributes: AudioAttributes? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var focusState: Boolean = false
    private var mAudioFocusPlaybackDelayed: Boolean = false
    private var mAudioFocusResumeOnFocusGained: Boolean = false
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

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
            recordingMode = true

            //Audio manager:
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
                    mAudioFocusPlaybackDelayed = true
                }
            }

        } catch (e: Exception) {
            Log.d(TAG, "Exception: ", e)
            recordingMode = false
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
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.DJames"
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
            .setContentTitle("DJames: Voice Search Service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    fun countdownStart() {

        //prepare thread:
        val mThread = Thread {
            try {
                synchronized(this) {
                    //RECORDING:
                    Log.d(TAG, "RECORDING STARTED.")
                    //Set overlay BUSY color:
                    if (screenOn && overlayButton != null) {
                        overlayButton!!.setBackgroundResource(R.drawable.rounded_button_busy)
                        overlayIcon!!.setImageResource(R.drawable.speak_icon)
                    }

                    //Play START tone:
                    toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)

                    //Start recording (default: cacheDir, alternative: saveDir):
                    var it = File(cacheDir, "audio.mp3")
                    recorder.start(it)

                    //Countdown:
                    Thread.sleep(prefs.recTimeout.toLong() * 1000)   //default: 5000

                    //Stop recording:
                    recorder.stop()
                    Log.d(TAG, "RECORDING STOPPED.")

                    //Play STOP tone:
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)

                    //Lower volume if maximum (to enable Receiver):
                    if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                        audioManager!!.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                        Log.d(TAG, "Countdown stopped. Volume lowered.")
                    }

                    //Abandon audio focus:
                    //if (!focusState) {}
                    audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)

                    //Set overlay PROCESSING color & icon:
                    if (screenOn && overlayButton != null && overlayIcon != null) {
                        overlayButton!!.setBackgroundResource(R.drawable.rounded_button_processing)
                        overlayIcon!!.setImageResource(R.drawable.looking_icon)
                    }

                    //AFTER RECORDING:
                    //Spotify result:
                    var spotifyToOpen =
                        "https://open.spotify.com/track/6atVS7UZBxoyJkkteM62u5?si=rLCB_v7EQrSTfTXe2QLqQg"
                    //Open links and redirects:
                    openResults(spotifyToOpen)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
                recordingMode = false
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

        if (prefs.mapsTimeout.toLong() > 0) {
            //Maps redirect:
            Thread.sleep((prefs.mapsTimeout.toLong()-1) * 1000)   //default: 3000
            //Launch Maps:
            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(prefs.mapsAddress)
            )
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mapIntent.putExtra("fromwhere", "ser")
            startActivity(mapIntent)
        } else if (prefs.clockTimeout.toLong() > 0) {
            //Clock redirect:
            Thread.sleep((prefs.clockTimeout.toLong()-1) * 1000)   //default: 10000
            //Launch Clock:
            val clockIntent = Intent(this, FakeLockScreen::class.java)
            clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clockIntent.putExtra("fromwhere", "ser")
            startActivity(clockIntent)
        }

        //Stop Voice Search service:
        recordingMode = false
        //Reset normal overlay ACCENT color & icon:
        if (screenOn && overlayButton != null && overlayIcon != null) {
            Thread.sleep(1000)   //default: 2000
            if (clock_active) {
                overlayButton!!.setBackgroundResource(R.drawable.rounded_button_dark)
                overlayIcon!!.setImageResource(R.drawable.speak_icon_gray)
            } else {
                overlayButton!!.setBackgroundResource(R.drawable.rounded_button_ready)
                overlayIcon!!.setImageResource(R.drawable.speak_icon)
            }
        }
        stopSelf()
    }

}
