package com.ftrono.DJames.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.R
import com.ftrono.DJames.api.NLPInterpreter
import com.ftrono.DJames.api.NLPQuery
import com.ftrono.DJames.api.SpotifyInterpreter
import com.ftrono.DJames.application.*
import com.ftrono.DJames.recorder.AndroidAudioRecorder
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class VoiceSearchService : Service() {
    //Main:
    private val TAG = VoiceSearchService::class.java.simpleName
    private var utils = Utilities()
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private var vqThreadExt: Thread? = null
    private var recFile: File? = null
    private var logFile: File? = null
    private var intentName = ""
    private var reqQueryLanguage = ""
    private var reqMessLanguage = ""
    private var fullMessLanguage = ""
    private var contact = JsonObject()
    private var contact_name = ""
    private var phone = ""
    private var intStarted = false
    private var messIntStarted = false
    private var messageModeOn = false

    //Recorder:
    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //API callers:
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

    //THREADS:
    private val vqThreadRec = Thread {
        try {
            voiceQueryRecorder()
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
            stopSelf()
        }
    }

    private val vqThreadMessRec = Thread {
        try {
            voiceQueryRecorder(messageMode = true)
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
            stopSelf()
        }
    }

    private val vqThreadInt = Thread {
        try {
            synchronized(this) {
                voiceQueryInterpreter()
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    private val vqThreadMessInt = Thread {
        try {
            synchronized(this) {
                voiceQueryInterpreter(messageMode = true)
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()
            voiceSearchOn = true

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
                    //START VOICE SEARCH RECORDER:
                    vqThreadRec.start()
                }

                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    mAudioFocusPlaybackDelayed = true
                }
            }

            //API caller:
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
            voiceSearchOn = false
            searchFail = false
            sourceIsVolume = false
            //Threads check:
            if (vqThreadRec.isAlive()) {
                vqThreadRec.interrupt()
            }
            if (vqThreadInt.isAlive()) {
                vqThreadInt.interrupt()
            }
            if (vqThreadMessRec.isAlive()) {
                vqThreadMessRec.interrupt()
            }
            if (vqThreadMessInt.isAlive()) {
                vqThreadMessInt.interrupt()
            }
            //Stop recorder:
            if (recordingMode) {
                try {
                    recFile = recorder.stop()
                } catch (e: Exception) {
                    Log.d(TAG, "Recorder not available.")
                }
            }
            //unregister receiver:
            try {
                unregisterReceiver(VSReceiver)
            } catch (e: Exception) {
                Log.d(TAG, "VQReceiver already unregistered.")
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
        if (recordingMode) {
            try {
                recFile = recorder.stop()
            } catch (e: Exception) {
                Log.d(TAG, "Recorder not available.")
            }
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        }
        //Threads check:
        if (vqThreadRec.isAlive()) {
            vqThreadRec.interrupt()
        }
        if (vqThreadInt.isAlive()) {
            vqThreadInt.interrupt()
        }
        if (vqThreadMessRec.isAlive()) {
            vqThreadMessRec.interrupt()
        }
        if (vqThreadMessInt.isAlive()) {
            vqThreadMessInt.interrupt()
        }
        try {
            //Abandon audio focus:
            audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
        } catch (e: Exception) {
            Log.d(TAG, "Audio focus already released.")
        }
        //unregister receiver:
        try {
            unregisterReceiver(VSReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "VQReceiver already unregistered.")
        }
        //Reset:
        searchFail = false
        recordingMode = false
        voiceSearchOn = false
        sourceIsVolume = false
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_OVERLAY_READY)
            sendBroadcast(intent)
        }
        Log.d(TAG, "VOICE SEARCH SERVICE TERMINATED.")
    }


    //VOICE QUERY FUNCTIONS:
    fun voiceQueryRecorder(messageMode: Boolean = false) {

        //1) RECORDING:
        Log.d(TAG, "RECORDING STARTED.")
        //Set overlay BUSY color:
        Intent().also { intent ->
            intent.setAction(ACTION_OVERLAY_BUSY)
            sendBroadcast(intent)
        }

        if (messageMode) {
            //Play MESSAGE REC tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP)   //MESSAGE REC
        } else {
            //Play START tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)   //START
        }

        //Start recording (default: cacheDir, alternative: saveDir):
        recordingMode = true
        recorder.start(saveDir)

        //Start personal Receiver:
        Thread.sleep(1000)
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_REC_EARLY_STOP)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(VSReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "VSReceiver started.")

        //Remaining rec countdown:
        if (messageMode) {
            Thread.sleep((prefs.messageTimeout.toLong() -1) * 1000)   //default: 30000
        } else {
            Thread.sleep((prefs.recTimeout.toLong() -1) * 1000)   //default: 5000
        }

        //START INTERPRETER:
        if (messageMode && !messIntStarted) {
            Intent().also { intent ->
                intent.setAction(ACTION_REC_EARLY_STOP)
                sendBroadcast(intent)
            }
        } else if (!messageMode && !intStarted) {
            Intent().also { intent ->
                intent.setAction(ACTION_REC_EARLY_STOP)
                sendBroadcast(intent)
            }
        }
    }


    fun voiceQueryInterpreter(messageMode: Boolean = false) {

        //INTERPRETER:
        //Stop recording:
        recordingMode = false
        recFile = recorder.stop()
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

            var nlp_fail = false

            //1st NLP Query (default language):
            var nlpQuery = NLPQuery(applicationContext)
            var resultsNLP = nlpQuery.queryNLP(recFile!!, messageMode=messageMode, reqLanguage=reqMessLanguage)
            resultsNLP.addProperty("query_no", 1)
            //1st trial: Get query_text:
            try {
                nlp_queryText = resultsNLP.get("query_text").asString
            } catch (e: Exception) {
                nlp_queryText = ""
            }
            //1st trial: Get DF auto-response:
            try {
                intentName = resultsNLP.get("intent_response").asString
            } catch (e: Exception) {
                intentName = ""
                Log.d(TAG, "No intent_response in NLP query result.")
            }

            //CHECK SWITCH LANGUAGE:
            if (!messageMode) {
                if (intentName == "MessageRequest") {
                    try {
                        //Get requested messaging language:
                        val reader = BufferedReader(InputStreamReader(applicationContext.resources.openRawResource(R.raw.languages)))
                        val sourceMap = JsonParser.parseReader(reader).asJsonObject
                        fullMessLanguage = resultsNLP.get("language").asString
                        reqMessLanguage = sourceMap[fullMessLanguage].asString
                    } catch (e: Exception) {
                        fullMessLanguage = ""
                        reqMessLanguage = ""
                    }
                    Log.d(TAG, "REQUESTED MESSAGING LANGUAGE: $reqMessLanguage")

                } else if (nlp_queryText != "") {
                    //Extract requested query language and transcribe again:
                    reqQueryLanguage = utils.checkLanguageSwitch(applicationContext, resultsNLP)
                    Log.d(TAG, "REQUESTED QUERY LANGUAGE: $reqQueryLanguage")
                    if (reqQueryLanguage != "") {
                        //2nd NLP Query, in the new requested language (overwrite):
                        nlpQuery = NLPQuery(applicationContext)
                        resultsNLP = nlpQuery.queryNLP(recFile!!, messageMode=false, reqLanguage=reqQueryLanguage)
                        resultsNLP.addProperty("query_no", 2)

                        //2nd trial: Get query_text:
                        try {
                            nlp_queryText = resultsNLP.get("query_text").asString
                        } catch (e: Exception) {
                            nlp_queryText = ""
                        }
                        //2nd trial: Get DF auto-response:
                        try {
                            intentName = resultsNLP.get("intent_response").asString
                        } catch (e: Exception) {
                            intentName = ""
                            Log.d(TAG, "No intent_response in NLP query result.")
                        }
                    }
                }
            }
            resultsNLP.addProperty("reqLanguage", reqQueryLanguage)

            //PROCESS:
            if (nlp_queryText == "") {
                //Empty response:
                nlp_fail = true
            } else {
                //Fallback cases:
                if (!messageMode && intentName == "") {
                    nlp_fail = true
                } else if (!messageMode && intentName.lowercase() == "fallback") {
                    nlp_fail = true
                } else if (messageMode && intentName.lowercase() == "cancel") {
                    nlp_fail = true
                }
            }

            if (!messageMode) {
                //Prepare toast text:
                var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }
                if (toastText == "") {
                    toastText = "Sorry, I did not understand!"
                }
                //CALL OR MESSAGE REQUEST -> replace actual contact name:
                if (intentName == "CallRequest" || intentName == "MessageRequest") {
                    val nlpInterpreter = NLPInterpreter(applicationContext)
                    if (intentName == "MessageRequest") {
                        //Message:
                        contact = nlpInterpreter.extractContact(nlp_queryText, fullLanguage=fullMessLanguage)
                    } else {
                        //Call:
                        contact = nlpInterpreter.extractContact(nlp_queryText)
                    }
                    //Extract:
                    contact_name = contact.get("contact_confirmed").asString
                    phone = contact.get("contact_phone").asString
                    //Replace:
                    if (contact_name != "") {
                        toastText = toastText.replace(contact.get("contact_extracted").asString, contact_name.uppercase())
                    }
                }
                //TOAST -> Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_TOASTER)
                    intent.putExtra("toastText", toastText)
                    sendBroadcast(intent)
                }
            }

            if (nlp_fail) {
                //NLP FAIL:
                //Play FAIL tone:
                toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                stopSelf()

            } else if (messageMode) {
                // MESSAGE SENDER:
                try {
                    //Send SMS:
                    val smsManager: SmsManager = SmsManager.getDefault()
                    var messageText = utils.replaceEmojis(context=applicationContext, text=nlp_queryText, reqLanguage=reqMessLanguage)

                    val parts = smsManager.divideMessage(messageText)
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                    //smsManager.sendTextMessage(phone, null, messageText, null, null)

                    //SUCCESS -> Play ACKNOWLEDGE tone:
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE

                    //TOAST -> Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_TOASTER)
                        intent.putExtra("toastText", "SMS sent to ${contact_name.uppercase()}")
                        sendBroadcast(intent)
                    }

                } catch (e: Exception) {
                    //Play FAIL tone:
                    toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                    //TOAST -> Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_TOASTER)
                        intent.putExtra("toastText", "ERROR: SMS not sent!")
                        sendBroadcast(intent)
                    }
                }
                stopSelf()

            } else {
                //B.2) ANSWER TO REQUEST:
                logFile = File(logDir, "$now.json")

                if (intentName == "CallRequest") {
                    //A) PHONE CALL:
                    //Close log:
                    logFile!!.writeText(last_log.toString())
                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_LOG_REFRESH)
                        sendBroadcast(intent)
                    }
                    if (phone == "") {
                        //Play FAIL tone:
                        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                    } else {
                        //Play ACKNOWLEDGE tone:
                        toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                        //CALL:
                        Intent().also { intent ->
                            intent.setAction(ACTION_MAKE_CALL)
                            intent.putExtra("toCall", "tel:${phone}")
                            sendBroadcast(intent)
                        }
                    }
                    stopSelf()

                } else if (intentName == "MessageRequest") {
                    //B) MESSAGE:
                    //Close log:
                    logFile!!.writeText(last_log.toString())
                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_LOG_REFRESH)
                        sendBroadcast(intent)
                    }
                    if (phone == "") {
                        //Play FAIL tone:
                        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                        stopSelf()
                    } else {
                        //START MESSAGE RECORDING:
                        if (voiceSearchOn) {
                            messageModeOn = true
                            vqThreadMessRec.start()
                        } else {
                            //Play FAIL tone:
                            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                            stopSelf()
                        }
                    }

                } else if (intentName == "LikeRequest") {
                    //C) LIKE REQUEST:
                    /////////////////////////////////////////
                    //TEMP:
                    //Play ACKNOWLEDGE tone:
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    //Close log:
                    logFile!!.writeText(last_log.toString())
                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_LOG_REFRESH)
                        sendBroadcast(intent)
                    }
                    stopSelf()

                } else {
                    //D) PLAY REQUEST:
                    var queryResult: JsonObject =
                        spotifyInterpreter!!.dispatchCall(resultsNLP, reqLanguage=reqQueryLanguage)

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
                        Thread.sleep(1000)

                        //Overwrite player info:
                        last_log!!.add("spotify_play", queryResult)
                        //Close log:
                        logFile!!.writeText(last_log.toString())

                        //Send broadcast:
                        Intent().also { intent ->
                            intent.setAction(ACTION_LOG_REFRESH)
                            sendBroadcast(intent)
                        }

                        //C) PLAY:
                        var playType = queryResult.get("play_type").asString
                        //TRIAL 1:
                        //Try requested context:
                        var sessionState =
                            spotifyInterpreter!!.playInternally(queryResult, useAlbum=false)
                        Log.d(TAG, "(FIRST) SESSION STATE: ${sessionState}")

                        if (sessionState == 0) {
                            if (queryResult.get("context_type").asString == "album") {
                                //If context was "album" -> terminate:
                                stopSelf()

                            } else {
                                //CUSTOM CONTEXT:
                                //Wait 1 sec:
                                Thread.sleep(1000)
                                //CHECK 204:
                                var playerState = spotifyInterpreter!!.getPlaybackState()
                                Log.d(TAG, "PLAYBACK STATE: $playerState")

                                if (playerState == 200) {
                                    //200: OK -> Terminate:
                                    stopSelf()

                                } else {
                                    //204: WRONG CONTEXT or 400-on:
                                    //TRIAL 2:
                                    //Use album as context:
                                    sessionState = spotifyInterpreter!!.playInternally(queryResult, useAlbum = true)
                                    Log.d(TAG, "(SECOND) SESSION STATE: ${sessionState}")

                                    //Update log:
                                    last_log!!.addProperty("context_error", true)
                                    logFile!!.writeText(last_log.toString())

                                    //Send broadcast:
                                    Intent().also { intent ->
                                        intent.setAction(ACTION_LOG_REFRESH)
                                        sendBroadcast(intent)
                                    }

                                    if (sessionState == 0) {
                                        //OK -> Terminate:
                                        stopSelf()

                                    } else {
                                        //400-on: OPEN EXTERNALLY WITH CONTEXT = ALBUM:
                                        //Open externally:
                                        var spotifyUrl =
                                            queryResult.get("spotify_URL").asString
                                        if (playType != "track") {
                                            openExternally(spotifyUrl)
                                        } else {
                                            var contextUri =
                                                queryResult.get("album_uri").asString
                                            val encodedContextUri: String =
                                                URLEncoder.encode(contextUri, "UTF-8")
                                            openExternally("$spotifyUrl?context=$encodedContextUri")
                                        }
                                    }
                                }
                            }

                        } else {
                            //400-on: OPEN EXTERNALLY WITH CONTEXT = ALBUM:
                            //Open externally:
                            var spotifyUrl = queryResult.get("spotify_URL").asString
                            if (playType != "track") {
                                openExternally(spotifyUrl)
                            } else {
                                var contextUri = queryResult.get("album_uri").asString
                                val encodedContextUri: String =
                                    URLEncoder.encode(contextUri, "UTF-8")
                                openExternally("$spotifyUrl?context=$encodedContextUri")
                            }
                            //Update log:
                            last_log!!.addProperty("play_externally", true)
                            logFile!!.writeText(last_log.toString())

                            //Send broadcast:
                            Intent().also { intent ->
                                intent.setAction(ACTION_LOG_REFRESH)
                                sendBroadcast(intent)
                            }
                        }
                    }
                }
            }
        }
    }


    private fun openExternally(spotifyToOpen: String) {
        //prepare thread:
        vqThreadExt = Thread {
            try {
                synchronized(this) {
                    //Open query result in Spotify:
                    val intentSpotify = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(spotifyToOpen)
                    )
                    intentSpotify.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intentSpotify.putExtra("fromwhere", "ser")
                    startActivity(intentSpotify)
                    stopSelf()

                    if (clock_active && prefs.clockRedirectEnabled) {
                        //Send broadcast:
                        Intent().also { intent ->
                            intent.setAction(ACTION_REDIRECT)
                            sendBroadcast(intent)
                        }
                        //Clock redirect:
                        Thread.sleep((prefs.clockTimeout.toLong() - 1) * 1000)   //default: 10000
                        //Launch Clock:
                        val clockIntent = Intent(this, FakeLockScreen::class.java)
                        clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        clockIntent.putExtra("fromwhere", "ser")
                        startActivity(clockIntent)
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
                stopSelf()
            }
        }
        //start thread:
        vqThreadExt!!.start()
    }


    //PERSONAL RECEIVER:
    private var VSReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Early stop Recording:
            if (intent!!.action == ACTION_REC_EARLY_STOP && recordingMode) {
                Log.d(TAG, "VSRECEIVER: EARLY STOP REC -> ACTION_REC_EARLY_STOP.")
                try {
                    //Start Voice Query Interpreter:
                    if (messageModeOn && !messIntStarted) {
                        vqThreadMessInt.start()
                        messIntStarted = true
                    } else if (!messageModeOn && !intStarted) {
                        vqThreadInt.start()
                        intStarted = true
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "VSRECEIVER: voiceQueryInterpreter not started. ", e)
                }
            }
        }
    }

}
