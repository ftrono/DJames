package com.ftrono.DJames.application.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.chatLastDispatch
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.application.recordingFail
import com.ftrono.DJames.application.recordingTime
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.application.voiceConvStarted
import com.ftrono.DJames.be.agents.AgentsGraph
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.NLPDispatcher
import com.ftrono.DJames.be.audio.AndroidAudioRecorder
import com.ftrono.DJames.be.audio.AudioRequestsManager
import com.ftrono.DJames.be.audio.TTSReader
import com.ftrono.DJames.be.chat.ActionsExecutor
import java.io.File


class VoiceQueryService: Service() {

    //Main:
    private val TAG = VoiceQueryService::class.java.simpleName
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val audioRequestsManager = AudioRequestsManager()
    private lateinit var tts: TTSReader
    private lateinit var agentGraph: AgentsGraph
    private lateinit var nlpDispatcher: NLPDispatcher

    //Recorder:
    private val MyRecorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //Status:
    private var followUp = false
    private var messageMode = false
    private var lastDispatch = DispatcherInfo()

    //JOBS:
    private var recordingThread: Thread? = null
    private var processingThread: Thread? = null

    // SERVICE:
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()
            voiceQueryOn = true
            tts = TTSReader(applicationContext)
            Log.d(TAG, "VOICE QUERY SERVICE STARTED.")

            val actFilter = IntentFilter()
            actFilter.addAction(ACTION_REC_STOP)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(VQReceiver, actFilter, RECEIVER_EXPORTED)
            Log.d(TAG, "VQReceiver started.")

            // Init conv orchestrator:
            agentGraph = AgentsGraph(applicationContext)
            nlpDispatcher = NLPDispatcher(applicationContext, agentGraph)

            //Start recording:
            startVoiceRecording(speakIntro = true)


        } catch (e: Exception) {
            Log.w(TAG, "Exception: ", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            MyRecorder.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Recorder not available.")
        }
        //Stop jobs:
        cancelThread(recordingThread, "recordingThread")
        cancelThread(processingThread, "processingThread")

        //Stop recorder:
        if (recordingMode || queryStatus.value != "ready") {
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        }
        //Abandon audio focus:
        audioRequestsManager.releaseDuckedFocus()
        audioRequestsManager.releaseExclusiveFocus()

        //unregister receiver:
        try {
            unregisterReceiver(VQReceiver)
            Log.d(TAG, "VQReceiver unregistered.")
        } catch (e: Exception) {
            Log.d(TAG, "VQReceiver already unregistered.")
        }

        //Reset:
        followUp = false
        messageMode = false
        recordingFail = false
        recordingMode = false
        voiceQueryOn = false
        sourceIsVolume.postValue(false)
        lastDispatch = DispatcherInfo()
        lastRequestIntent = ""
        lastRecordingName = ""
        voiceConvStarted = false
        //Set overlay READY color:
        queryStatus.postValue("ready")
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
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)!!
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


    fun cancelThread(thread: Thread?, threadName: String) {
        try {
            thread?.interrupt()
            Log.d(TAG, "Stopped $threadName!")
        } catch (e: Exception) {
            Log.w(TAG, "$threadName not active.")
        }
    }


    // INTERACTION FLOW:
    // START RECORDING:
    private fun startVoiceRecording(
        speakIntro: Boolean = false
    ) {
        Log.d(TAG, "startVoiceRecording() triggered!")
        recordingTime = 0L
        try {
            cancelThread(processingThread, "processingThread")
            //Start rec Job:
            recordingThread = Thread {
                // 1) SPEAK INTRO:
                if (voiceQueryOn && speakIntro) {

                    audioRequestsManager.requestDuckedFocus(
                        onGranted = {
                            //End previous conversations:
                            voiceConvStarted = false
                            lastRecordingName = ""
                            chatLastDispatch = DispatcherInfo()
                            //START:
                            queryStatus.postValue("processing")
                            Thread.sleep(500)
                            //Read TTS:
                            tts.speak(
                                listOf(
                                    AiReply(
                                        langCode = prefs.queryLanguage,
                                        text = defaultReplies.speakIntro()
                                    )
                                ),
                                saveMessage = false   // intro message!
                            )
                        },
                        onFail = { stopSelf() }
                    )
                    audioRequestsManager.releaseDuckedFocus()
                }

                // 2) RECORD:
                if (voiceQueryOn) {
                    audioRequestsManager.requestExclusiveFocus(
                        onGranted = {
                            //Set overlay BUSY color:
                            queryStatus.postValue("busy")

                            //Play START tone:
                            toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)   //START
                            // toneGen.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP)   //FOLLOW UP

                            //Start recording (default: cacheDir):
                            messageUtils.resetMessage(fromUser = true)
                            if (
                                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            ) {
                                MyRecorder.start(
                                    messageMode = messageMode,
                                    messageType = lastDispatch.messageType,
                                )
                            }
                        },
                        onFail = { stopSelf() }
                    )
                }
            }
            recordingThread!!.start()

        } catch (e: Exception) {
            Log.w(TAG, "VQSERVICE: EXCEPTION: ", e)
            stopSelf()
        }
    }


    // STOP RECORDING:
    fun stopVoiceRecording() {
        Log.d(TAG, "stopVoiceRecording() triggered")
        try {
            var recFile = MyRecorder.stop()
            Log.d(TAG, "RECORDING STOPPED.")
            cancelThread(recordingThread, "recordingThread")
            messageUtils.resetMessage(fromUser = false)

            //2) RECORDING RESULT:
            if (!voiceQueryOn || recordingFail) {
                //A) RECORDING FAIL -> END:
                stopSelf()

            } else {
                //B) RECORDING SUCCESS:
                //Play STOP tone:
                toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP
                audioRequestsManager.releaseExclusiveFocus()
                audioRequestsManager.requestDuckedFocus(
                    onGranted = {
                        if (voiceQueryOn) {
                            //Set overlay PROCESSING color & icon:
                            queryStatus.postValue("processing")
                            //PROCESS QUERY:
                            processingThread = Thread {
                                processQuery(recFile)
                            }
                            processingThread!!.start()
                        }
                    },
                    onFail = { stopSelf() }
                )

            }

        } catch (e: Exception) {
            Log.w(TAG, "VQSERVICE: EXCEPTION: ", e)
            stopSelf()
        }
    }


    // PROCESS QUERY:
    private fun processQuery(recFile: File) {
        Log.d(TAG, "PROCESSING JOB STARTED!")
        try {
            //PROCESS REQUEST:
            if (voiceQueryOn) {
                lastDispatch = nlpDispatcher.dispatch(recFile=recFile, prevDispatch=lastDispatch, fromVoice=true)
                messageMode = lastDispatch.messageMode
                followUp = lastDispatch.followUp
                val actionsExecutor = ActionsExecutor(applicationContext)
                var newReplies = listOf<AiReply>()

                if (lastDispatch.fail && lastDispatch.aiReplies.isEmpty()) {
                    // Default fail replies:
                    newReplies = listOf(
                        AiReply(
                            langCode = prefs.queryLanguage,
                            text = defaultReplies.replyError()
                        )
                    )
                }

                // Speak & execute:
                val intentName = lastRequestIntent
                Thread.sleep(300)
                if (voiceQueryOn && intentName.contains("Play") || intentName.contains("Call")) {
                    // A) First speak, then execute action:
                    // Read:
                    if (lastDispatch.aiReplies.isNotEmpty()) {
                        tts.speak(lastDispatch.aiReplies)
                    }
                    audioRequestsManager.releaseDuckedFocus()
                    // Execute (only if end):
                    if (lastDispatch.end && lastDispatch.actionType != null) {
                        actionsExecutor.execute(lastDispatch)
                    }

                } else if (voiceQueryOn) {
                    // B) First execute action, then speak:
                    // Execute (only if end):
                    if (lastDispatch.end && lastDispatch.actionType != null) {
                        newReplies = actionsExecutor.execute(lastDispatch)
                    }
                    // If received updated replies: replace!
                    if (newReplies.isNotEmpty()) {
                        lastDispatch.aiReplies = newReplies
                    }
                    // Read:
                    if (lastDispatch.aiReplies.isNotEmpty()) {
                        tts.speak(lastDispatch.aiReplies)
                    }
                    audioRequestsManager.releaseDuckedFocus()

                } else {
                    stopSelf()
                }

                if (voiceQueryOn && (messageMode || followUp)) {
                    // START FOLLOWUP INTERACTION:
                    startVoiceRecording()

                } else {
                    // END:
                    queryStatus.postValue("ready")
                    if (lastDispatch.fail) {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                    } else {
                        if (lastDispatch.playAcknowledge) toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    }
                    Thread.sleep(200)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VQSERVICE: EXCEPTION: ", e)
            stopSelf()
        }
    }


    //PERSONAL RECEIVER:
    private var VQReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Stop Recording:
            if (intent!!.action == ACTION_REC_STOP && recordingMode && recordingTime >= 1L) {
                Log.d(TAG, "VQRECEIVER: ACTION_REC_STOP.")
                stopVoiceRecording()
            }

        }
    }

}
