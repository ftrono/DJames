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
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.application.*
import com.ftrono.DJames.api.NLPQuery
import com.ftrono.DJames.application.FakeLockScreen
import com.ftrono.DJames.recorder.AndroidAudioRecorder
import com.ftrono.DJames.api.SpotifyInterpreter
import com.google.gson.JsonObject
import java.net.URLEncoder
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class VoiceSearchService : Service() {
    //Main:
    private val TAG = VoiceSearchService::class.java.simpleName
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private var vqThread: Thread? = null
    private var recInProgress = false
    private var logFile: File? = null

    //Recorder:
    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //API callers:
    private var nlpQuery: NLPQuery? = null
    private var spotifyInterpreter: SpotifyInterpreter? = null

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

            //API callers:
            nlpQuery = NLPQuery(applicationContext)
            spotifyInterpreter = SpotifyInterpreter(applicationContext)

        } catch (e: Exception) {
            Log.e(TAG, "Exception: ", e)
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
            //Thread check:
            if (vqThread != null) {
                if (vqThread!!.isAlive()) {
                    vqThread!!.interrupt()
                }
            }
            //Stop recorder:
            if (recInProgress) {
                try {
                    var recFile = recorder.stop()
                } catch (e: Exception) {
                    Log.d(TAG, "Recorder not available.")
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        //Stop recorder:
        if (recInProgress) {
            try {
                var recFile = recorder.stop()
            } catch (e: Exception) {
                Log.d(TAG, "Recorder not available.")
            }
            //Thread check:
            if (vqThread != null) {
                if (vqThread!!.isAlive()) {
                    vqThread!!.interrupt()
                }
            }
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        }
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
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_OVERLAY_READY)
            sendBroadcast(intent)
        }
    }



    //MAIN VOICE QUERY CALLER:
    fun voiceQuery() {
        //prepare thread:
        vqThread = Thread {
            try {
                synchronized(this) {

                    //1) RECORDING:
                    Log.d(TAG, "RECORDING STARTED.")
                    //Set overlay BUSY color:
                    Intent().also { intent ->
                        intent.setAction(ACTION_OVERLAY_BUSY)
                        sendBroadcast(intent)
                    }

                    //Play START tone:
                    toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)   //START

                    //Start recording (default: cacheDir, alternative: saveDir):
                    recInProgress = true
                    recorder.start(saveDir)

                    //Countdown:
                    Thread.sleep(prefs.recTimeout.toLong() * 1000)   //default: 5000

                    //Stop recording:
                    var recFile = recorder.stop()
                    recInProgress = false
                    Log.d(TAG, "RECORDING STOPPED.")

                    //Lower volume if maximum (to enable Receiver):
                    if (sourceIsVolume && audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume-1, AudioManager.FLAG_PLAY_SOUND)
                        Log.d(TAG, "Countdown stopped. Volume lowered.")
                    }

                    //2) RECORDING RESULT:
                    if (searchFail) {
                        //A) RECORDING FAIL -> END:
                        //Play FAIL tone:
                        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                        stopSelf()
                    } else {
                        //B) RECORDING SUCCESS:
                        //Play STOP tone:
                        toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP

                        //Set overlay PROCESSING color & icon:
                        Intent().also { intent ->
                            intent.setAction(ACTION_OVERLAY_PROCESSING)
                            sendBroadcast(intent)
                        }

                        //B.1) NLP QUERY:
                        //Init last_log:
                        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        last_log = JsonObject()
                        last_log!!.addProperty("datetime", now)
                        last_log!!.addProperty("app_version", appVersion)

                        //Query NLP:
                        var resultsNLP = nlpQuery!!.queryNLP(recFile)

                        //Check NLP results:
                        nlp_queryText = ""
                        var nlp_fail = false
                        if (!resultsNLP.has("query_text")) {
                            //Empty response:
                            nlp_fail = true
                        } else {
                            //Get query_text:
                            nlp_queryText = resultsNLP.get("query_text").asString
                            if (nlp_queryText == "") {
                                //Empty string:
                                nlp_fail = true
                            }
                        }

                        //TOAST -> Send broadcast:
                        Intent().also { intent ->
                            intent.setAction(ACTION_NLP_RESULT)
                            sendBroadcast(intent)
                        }
                        if (nlp_fail) {
                            //NLP FAIL:
                            //Play FAIL tone:
                            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                            stopSelf()

                        } else {
                            //B.2) ANSWER TO REQUEST:
                            logFile = File(logDir, "$now.json")
                            val intentName = resultsNLP.get("intent").asString
                            last_log!!.addProperty("requestType", intentName)

                            if (intentName == "CallRequest") {
                                //A) PHONE CALL:
                                //////////////////////////
                                //TEMP:
                                //Play FAIL tone:
                                toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                                //Close log:
                                logFile!!.writeText(last_log.toString())
                                //Send broadcast:
                                Intent().also { intent ->
                                    intent.setAction(ACTION_LOG_REFRESH)
                                    sendBroadcast(intent)
                                }
                                stopSelf()

                            } else if (intentName == "LikeRequest") {
                                //B) LIKE REQUEST:
                                /////////////////////////////////////////
                                //TEMP:
                                //Play FAIL tone:
                                toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                                //Close log:
                                logFile!!.writeText(last_log.toString())
                                //Send broadcast:
                                Intent().also { intent ->
                                    intent.setAction(ACTION_LOG_REFRESH)
                                    sendBroadcast(intent)
                                }
                                stopSelf()

                            } else {
                                //C) PLAY REQUEST:
                                var queryResult: JsonObject =
                                    spotifyInterpreter!!.dispatchCall(resultsNLP)

                                //A) EMPTY QUERY RESULT:
                                if (!queryResult.has("uri")) {
                                    //Play FAIL tone:
                                    toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                                    //Close log:
                                    logFile!!.writeText(last_log.toString())
                                    //Send broadcast:
                                    Intent().also { intent ->
                                        intent.setAction(ACTION_LOG_REFRESH)
                                        sendBroadcast(intent)
                                    }
                                    stopSelf()
                                } else {
                                    //B) SPOTIFY RESULT RECEIVED!
                                    //Abandon audio focus:
                                    audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)

                                    //Wait 1 sec:
                                    Thread.sleep(1000)   //default: 5000

                                    //Overwrite player info:
                                    currently_playing = queryResult
                                    last_log!!.add("spotify_play", currently_playing)
                                    //Close log:
                                    logFile!!.writeText(last_log.toString())

                                    //Send broadcast:
                                    Intent().also { intent ->
                                        intent.setAction(ACTION_NEW_SONG)
                                        sendBroadcast(intent)
                                    }

                                    //Send broadcast:
                                    Intent().also { intent ->
                                        intent.setAction(ACTION_LOG_REFRESH)
                                        sendBroadcast(intent)
                                    }

                                    //C) PLAY:
                                    var sessionState =
                                        spotifyInterpreter!!.playInternally(queryResult)
                                    Log.d(TAG, "SESSION STATE: ${sessionState}")
                                    if (sessionState == -1) {
                                        //Open externally:
                                        var spotifyUrl = queryResult.get("spotify_URL").asString
                                        var contextUri = queryResult.get("context_uri").asString
                                        val encodedContextUri: String =
                                            URLEncoder.encode(contextUri, "UTF-8")
                                        openExternally("$spotifyUrl?context=$encodedContextUri")
                                        //openExternally(spotifyUrl)
                                    } else {
                                        stopSelf()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
                stopSelf()
            }
        }
        //start thread:
        vqThread!!.start()
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
        stopSelf()

        if (prefs.clockRedirectEnabled) {
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_REDIRECT)
                sendBroadcast(intent)
            }
            //Clock redirect:
            Thread.sleep((prefs.clockTimeout.toLong()-1) * 1000)   //default: 10000
            //Launch Clock:
            val clockIntent = Intent(this, FakeLockScreen::class.java)
            clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clockIntent.putExtra("fromwhere", "ser")
            startActivity(clockIntent)
        }
    }

}
