package com.ftrono.DJames.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.application.ACTION_OVERLAY_BUSY
import com.ftrono.DJames.application.ACTION_OVERLAY_PROCESSING
import com.ftrono.DJames.application.ACTION_OVERLAY_READY
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.audioAttributes
import com.ftrono.DJames.application.audioFocusChangeListener
import com.ftrono.DJames.application.audioFocusRequest
import com.ftrono.DJames.application.audioManager
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.application.saveDir
import com.ftrono.DJames.application.searchFail
import com.ftrono.DJames.application.silenceInitPatience
import com.ftrono.DJames.application.silencePatience
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.application.streamMaxVolume
import com.ftrono.DJames.nlp.NLPDispatcher
import com.ftrono.DJames.recorder.AndroidAudioRecorder
import com.ftrono.DJames.recorder.AudioRecorder
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import org.jetbrains.kotlinx.dataframe.math.std
import java.io.File
import kotlin.math.roundToInt


class VoiceQueryService: Service() {
    //Main:
    private val TAG = VoiceQueryService::class.java.simpleName
    private val utils = Utilities()
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    //Recorder:
    private var rec_time = 0
    private val MyRecorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }
    private var recThread: Thread? = null
    private var processThread: Thread? = null

    //Status:
    private var followUp = false
    private var messageMode = false
    private var processStatus = JsonObject()


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()
            voiceQueryOn = true
            Log.d(TAG, "VOICE QUERY SERVICE STARTED.")

            val actFilter = IntentFilter()
            //actFilter.addAction(ACTION_REC_START)
            actFilter.addAction(ACTION_REC_STOP)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(VQReceiver, actFilter, RECEIVER_EXPORTED)
            Log.d(TAG, "VQReceiver started.")

            //Start recording immediately:
            startRecording()


        } catch (e: Exception) {
            Log.e(TAG, "Exception: ", e)
            stopSelf()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //Stop:
        Intent().also { intent ->
            intent.setAction(ACTION_REC_STOP)
            applicationContext.sendBroadcast(intent)
        }
        //Stop recorder:
        if (recordingMode) {
            try {
                var recFile = MyRecorder.stop()
            } catch (e: Exception) {
                Log.d(TAG, "MyRecorder not available.")
            }
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        }
        //Stop threads:
        try {
            if (recThread!!.isAlive()) {
                recThread!!.interrupt()
            }
        } catch (e: Exception) {
            Log.d(TAG, "No RecThread active.")
        }
        try {
            if (processThread!!.isAlive()) {
                processThread!!.interrupt()
            }
        } catch (e: Exception) {
            Log.d(TAG, "No ProcessThread active.")
        }
        //Abandon audio focus:
        utils.releaseAudioFocus()
        //unregister receiver:
        try {
            unregisterReceiver(VQReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "VQReceiver already unregistered.")
        }
        //Reset:
        searchFail = false
        recordingMode = false
        voiceQueryOn = false
        sourceIsVolume = false
        processStatus = JsonObject()
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_OVERLAY_READY)
            sendBroadcast(intent)
        }
        Log.d(TAG, "VOICE QUERY SERVICE TERMINATED.")
    }


    private fun startForeground() {
        //Foreground service:
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.DJames"
        val channelName = "Voice Query Service"
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
            .setContentTitle("DJames: Voice Query Service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }


    //FUNCTIONS:
    //Helper:
    private fun record() {
        //START VOICE RECORDER:
        Log.d(TAG, "RECORDING STARTED.")

        //Set overlay BUSY color:
        Intent().also { intent ->
            intent.setAction(ACTION_OVERLAY_BUSY)
            sendBroadcast(intent)
        }

        if (followUp || messageMode) {
            //Play FOLLOW UP tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP)   //FOLLOW UP
        } else {
            //Play START tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)   //START
        }

        //Start recording (default: cacheDir, alternative: saveDir):
        recordingMode = true
        rec_time = 0
        MyRecorder.start(saveDir)

        //Start rec Thread:
        recThread = Thread {
            whileRecording(MyRecorder, messageMode=messageMode)
        }
        recThread!!.start()
    }


    //Start recording:
    private fun startRecording() {
        try {
            if (followUp) {
                //START RECORDING with no new audiofocus request:
                record()

            } else {
                //PREPARE AUDIOFOCUS REQUEST:
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

                        //START RECORDING:
                        record()
                    }

                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        //mAudioFocusPlaybackDelayed = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "VQSERVICE: EXCEPTION: $e")
            fail()
        }
    }


    //Stop recording & process request:
    private fun stopAndProcess(followUp: Boolean = false) {
        try {
            //Stop recording:
            recordingMode = false
            var recFile = MyRecorder.stop()
            Log.d(TAG, "RECORDING STOPPED.")

            //Lower volume if maximum (to enable Receiver):
            if (sourceIsVolume && audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume - 1, AudioManager.FLAG_PLAY_SOUND)
                Log.d(TAG, "Countdown stopped. Volume lowered.")
            }

            //2) RECORDING RESULT:
            if (searchFail) {
                //A) RECORDING FAIL -> END:
                fail()

            } else {
                //B) RECORDING SUCCESS:
                //Play STOP tone:
                toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP

                //Set overlay PROCESSING color & icon:
                Intent().also { intent ->
                    intent.setAction(ACTION_OVERLAY_PROCESSING)
                    sendBroadcast(intent)
                }

                //PROCESS REQUEST:
                processThread = Thread {
                    synchronized(this) {
                        processResults(recFile)
                    }
                }
                processThread!!.start()
            }

        } catch (e: Exception) {
            Log.d(TAG, "VQSERVICE: EXCEPTION: $e")
            fail()
        }
    }


    private fun processResults(recFile: File) {
        //CALL NLP DISPATCHER:
        var nlpDispatcher = NLPDispatcher(applicationContext)
        processStatus = nlpDispatcher.dispatch(recFile, processStatus, followUp, messageMode)

        if (processStatus.isEmpty || processStatus.has("fail")) {
            var toastText = ""
            if (processStatus.has("toastText")) {
                toastText = processStatus.get("toastText").asString
            }
            fail(toastText)

        } else if (processStatus.has("stopService")) {
            if (processStatus.has("stopSound")) {
                //SUCCESS -> Play ACKNOWLEDGE tone:
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
            }
            //Gracefully stop the service:
            stopSelf()

        } else if (processStatus.has("messageMode")) {
            messageMode = processStatus.get("messageMode").asBoolean
            //SUPPOSED TO BE TRUE:
            try {
                startRecording()
            } catch (e: Exception) {
                Log.d(TAG, "VQRECEIVER: recording not started. ", e)
                fail()
            }

        } else if (processStatus.has("followUp")) {
            followUp = processStatus.get("followUp").asBoolean
            //SUPPOSED TO BE TRUE:
            try {
                startRecording()
            } catch (e: Exception) {
                Log.d(TAG, "VQRECEIVER: recording not started. ", e)
                fail()
            }
        } else {
            //TEMP:
            stopSelf()
        }
    }


    private fun fail(toastText: String = "") {
        //Play FAIL tone:
        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        if (toastText != "") {
            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", toastText)
                sendBroadcast(intent)
            }
        }
        followUp = false
        messageMode = false
        stopSelf()
    }


    //While recording:
    private fun whileRecording(recorder: AudioRecorder, messageMode: Boolean = false) {
        try {
            var c = 0
            var deltaThreshold = 5
            var amplitudes = mutableListOf<Int>()
            var min = 0
            var max = 0
            var std = 0
            var curAmpl = 0
            var maxTime = prefs.recTimeout.toLong()
            if (messageMode) {
                maxTime = prefs.messageTimeout.toLong()
            }

            //ONCE EVERY SECOND:
            while (rec_time < maxTime) {

                if (!recordingMode && rec_time > 0) {
                    //Recording is over:
                    break

                } else if (prefs.silenceEnabled) {
                    //Get max amplitude detected (in %):
                    curAmpl = recorder.getMaxAmplitude()
                    amplitudes.add(curAmpl)
                    Log.d(TAG, "CURRENT: $curAmpl")

                    try {
                        min = amplitudes.filter { it > 0 }.min()
                        max = amplitudes.max()
                        std = amplitudes.filter { it > 0 }.std().roundToInt()
                    } catch (e: Exception) {
                        Log.d(TAG, "No min/max.")
                    }

                    //Tolerance period ended:
                    if (rec_time == silenceInitPatience) {

                        if ((max - min) <= deltaThreshold) {
                            //Early stop!
                            Log.d(TAG, "RECORDER: SILENCE DETECTED! -> EARLY STOP")
                            Intent().also { intent ->
                                intent.setAction(ACTION_REC_STOP)
                                applicationContext.sendBroadcast(intent)
                            }
                            break

                        } else {
                            c = 0
                        }

                    } else if (rec_time > silencePatience) {

                        if ((max - curAmpl) <= std) {
                            //Go on:
                            c = 0

                        } else if (c >= silencePatience) {
                            //Early stop!
                            Log.d(TAG, "RECORDER: SILENCE DETECTED! -> EARLY STOP")
                            Intent().also { intent ->
                                intent.setAction(ACTION_REC_STOP)
                                applicationContext.sendBroadcast(intent)
                            }
                            break

                        } else {
                            //Speaking -> go on:
                            c++
                        }
                    }
                }
                Thread.sleep(1000)
                rec_time ++
            }

            Log.d(TAG, "AMPLITUDES: $amplitudes")
            Log.d(TAG, "MIN: $min")
            Log.d(TAG, "MAX: $max")
            Log.d(TAG, "STD: $std")

            if (recordingMode) {
                //STOP recording:
                Intent().also { intent ->
                    intent.setAction(ACTION_REC_STOP)
                    sendBroadcast(intent)
                }
            }

        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    
    //PERSONAL RECEIVER:
    private var VQReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Stop Recording:
            if (intent!!.action == ACTION_REC_STOP && recordingMode && rec_time >= 1) {
                Log.d(TAG, "VQRECEIVER: ACTION_REC_STOP.")
                try {
                    stopAndProcess()
                } catch (e: Exception) {
                    Log.d(TAG, "VQRECEIVER: recording not stopped. ", e)
                }
            }

        }
    }
}