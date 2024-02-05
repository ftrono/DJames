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
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.application.*
import com.ftrono.DJames.R
import com.ftrono.DJames.api.NLPInterpreter
import com.ftrono.DJames.application.FakeLockScreen
import com.ftrono.DJames.recorder.AndroidAudioRecorder
import com.ftrono.DJames.api.SpotifyInterpreter
import com.google.gson.JsonObject
import java.io.File


class VoiceSearchService : Service() {
    //Main:
    private val TAG = VoiceSearchService::class.java.simpleName
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
//    private val saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    //Recorder:
    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //API callers:
    private var nlpInterpreter = NLPInterpreter()
    private var spotifyInterpreter = SpotifyInterpreter()

    //Audio manager:
    private var audioAttributes: AudioAttributes? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var focusState: Boolean = false
    private var mAudioFocusPlaybackDelayed: Boolean = false
    private var mAudioFocusResumeOnFocusGained: Boolean = false

    //AudioFocus:
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
                    Log.d(TAG, "Cannot gain audio focus! Try again.")
                }

                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    //START VOICE SEARCH:
                    voiceQuery()
                }

                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    mAudioFocusPlaybackDelayed = true
                }
            }

        } catch (e: Exception) {
            Log.d(TAG, "Exception: ", e)
            //Abandon audio focus:
            try {
                audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
            } catch (e: Exception) {
                Log.d(TAG, "AudioFocus already released!")
            }
            //Reset:
            recordingMode = false
            searchFail = false
            sourceIsVolume = false
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


    //MAIN VOICE QUERY CALLER:
    fun voiceQuery() {
        //prepare thread:
        val mThread = Thread {
            try {
                synchronized(this) {

                    //1) RECORDING:
                    Log.d(TAG, "RECORDING STARTED.")
                    //Set overlay BUSY color:
                    setOverlayBusy()

                    //Play START tone:
                    toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)   //START

                    //Start recording (default: cacheDir, alternative: saveDir):
                    var recFile = File(cacheDir, "audio.mp3")
                    recorder.start(recFile)

                    //Countdown:
                    Thread.sleep(prefs.recTimeout.toLong() * 1000)   //default: 5000

                    //Stop recording:
                    recorder.stop()
                    Log.d(TAG, "RECORDING STOPPED.")

                    //Lower volume if maximum (to enable Receiver):
                    if (sourceIsVolume && audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                        audioManager!!.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                        Log.d(TAG, "Countdown stopped. Volume lowered.")
                    }

                    //2) RECORDING RESULT:
                    if (searchFail) {
                        //A) RECORDING FAIL -> END:
                        //Play FAIL tone:
                        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                        //Abandon audio focus:
                        audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
                        //Reset:
                        searchFail = false
                        recordingMode = false
                        sourceIsVolume = false
                        setOverlayReady()
                        stopSelf()
                    } else {
                        //B) RECORDING SUCCESS:
                        //Play STOP tone:
                        toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP

                        //Set overlay PROCESSING color & icon:
                        setOverlayProcessing()

                        //B.1) NLP QUERY:
                        var resultsNLP = nlpInterpreter.queryNLP(recFile)

                        //B.2) SPOTIFY QUERY:
                        var queryResult: JsonObject = spotifyInterpreter.dispatchCall(resultsNLP)

                        //A) EMPTY QUERY RESULT:
                        if (!queryResult.has("uri")) {
                            //Play FAIL tone:
                            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                            //Abandon audio focus:
                            audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
                            //Reset:
                            recordingMode = false
                            sourceIsVolume = false
                            setOverlayReady()
                            stopSelf()
                        } else {
                            //B) SPOTIFY RESULT RECEIVED!

                            //Overwrite player info:
                            songName = queryResult.get("song_name").asString
                            artistName = queryResult.get("artist_name").asString
                            contextName = queryResult.get("context_name").asString

                            //Send broadcast:
                            Intent().also { intent ->
                                intent.setAction(ACTION_NEW_SONG)
                                sendBroadcast(intent)
                            }

                            //Abandon audio focus:
                            audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)

                            //C) PLAY:
                            var sessionState = spotifyInterpreter.playInternally(queryResult)
                            Log.d(TAG, "SESSION STATE: ${sessionState}")
                            if (sessionState == -1) {
                                //Open externally:
                                var spotifyUrl = queryResult.get("spotify_URL").asString
                                openExternally(spotifyUrl)
                            }

                            //Reset:
                            recordingMode = false
                            sourceIsVolume = false
                            setOverlayReady()
                            stopSelf()
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
                try {
                    //Abandon audio focus:
                    audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
                } catch (e: Exception) {
                    Log.d(TAG, "Audio focus already released.")
                }
                //Reset:
                searchFail = false
                recordingMode = false
                sourceIsVolume = false
                setOverlayReady()
                stopSelf()
            }
        }
        //start thread:
        mThread.start()
    }


    private fun openExternally(spotifyToOpen: String) {
        //Open query result in Spotify:
        val intentSpotify = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(spotifyToOpen)
        )
        intentSpotify.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentSpotify.putExtra("fromwhere", "ser")
        startActivity(intentSpotify)

        if (prefs.clockRedirectEnabled) {
            //Clock redirect:
            Thread.sleep((prefs.clockTimeout.toLong()-1) * 1000)   //default: 10000
            //Launch Clock:
            val clockIntent = Intent(this, FakeLockScreen::class.java)
            clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clockIntent.putExtra("fromwhere", "ser")
            startActivity(clockIntent)
        }
    }

    fun setOverlayReady() {
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
    }

    fun setOverlayBusy() {
        //Set overlay BUSY color:
        if (screenOn && overlayButton != null) {
            overlayButton!!.setBackgroundResource(R.drawable.rounded_button_busy)
            overlayIcon!!.setImageResource(R.drawable.speak_icon)
        }
    }

    fun setOverlayProcessing() {
        //Set overlay PROCESSING color & icon:
        if (screenOn && overlayButton != null && overlayIcon != null) {
            overlayButton!!.setBackgroundResource(R.drawable.rounded_button_processing)
            overlayIcon!!.setImageResource(R.drawable.looking_icon)
        }
    }

}
