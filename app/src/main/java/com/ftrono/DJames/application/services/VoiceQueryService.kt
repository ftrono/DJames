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
import com.ftrono.DJames.R
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastStarterId
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.application.recordingFail
import com.ftrono.DJames.application.recordingTime
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.be.agents.AgentsGraph
import com.ftrono.DJames.be.agents.IntentsGraph
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.audio.AndroidAudioRecorder
import com.ftrono.DJames.be.audio.AudioRequestsManager
import com.ftrono.DJames.be.audio.TTSReader
import com.ftrono.DJames.be.agents.chat.ActionsExecutor
import com.ftrono.DJames.be.models.RecDetails


class VoiceQueryService: Service() {

    //Main:
    private val TAG = this::class.java.simpleName
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val audioRequestsManager = AudioRequestsManager()
    private lateinit var tts: TTSReader
    private lateinit var agentsGraph: AgentsGraph
    private lateinit var intentsGraph: IntentsGraph

    //Recorder:
    private val MyRecorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //Status:
    private var lastState = StateInfo()   // Reset

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
            agentsGraph = AgentsGraph(applicationContext)
            agentsGraph.build()
            intentsGraph = IntentsGraph(applicationContext)
            intentsGraph.build()

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
        recordingFail = false
        recordingMode = false
        voiceQueryOn = false
        sourceIsVolume.postValue(false)
        lastState = StateInfo()
        lastStarterId = 0L
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
            .setSmallIcon(R.drawable.app_icon_notification)
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
                synchronized(this) {
                    // 1) SPEAK INTRO:
                    if (voiceQueryOn && speakIntro) {

                        audioRequestsManager.requestDuckedFocus(
                            onGranted = {
                                //START:
                                queryStatus.postValue("processing")
                                lastState = StateInfo()
                                Thread.sleep(500)
                                //Read TTS:
                                tts.speak(
                                    aiReplies = listOf(
                                        AiReply(
                                            langCode = prefs.queryLanguage,
                                            text = defaultReplies.speakIntro()
                                        )
                                    )
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
                                if (
                                    ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    MyRecorder.start(
                                        messageMode = lastState.messageMode,
                                        messageType = lastState.messageType,
                                    )
                                }
                            },
                            onFail = { stopSelf() }
                        )
                    }
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
            var recDetails = MyRecorder.stop()
            Log.d(TAG, "RECORDING STOPPED.")
            cancelThread(recordingThread, "recordingThread")

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
                                synchronized(this) {
                                    processQuery(recDetails)
                                }
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
    private fun processQuery(recDetails: RecDetails) {
        Log.d(TAG, "PROCESSING JOB STARTED!")
        try {
            //PROCESS REQUEST:
            lastState.lastRecording = recDetails.recName
            if (voiceQueryOn) {
                lastState = if (prefs.enableV3) {
                    agentsGraph.invoke(
                        recDetails = recDetails,
                        prevState = lastState,
                    )
                } else {
                    intentsGraph.invoke(
                        recDetails = recDetails,
                        prevState = lastState,
                    )
                }
                // Enable FollowUp:
                var end = lastState.next == END
                val followUp = !end && !lastState.fail

                val actionsExecutor = ActionsExecutor(applicationContext)
                var newReplies = listOf<AiReply>()

                if (lastState.fail && lastState.aiReplies.isEmpty()) {
                    // Default fail replies:
                    newReplies = listOf(
                        AiReply(
                            langCode = prefs.queryLanguage,
                            text = defaultReplies.replyError()
                        )
                    )
                }

                // Speak & execute:
                val intentName = lastState.intentName
                Thread.sleep(300)
                if (voiceQueryOn && intentName.contains("Play") || intentName.contains("Call")) {
                    // A) First speak, then execute action:
                    // Read:
                    if (lastState.aiReplies.isNotEmpty()) {
                        // Save reply:
                        if (!lastState.noSave) {
                            messageUtils.storeMessage(
                                context = applicationContext,
                                langCode = prefs.queryLanguage,
                                fromUser = false,
                                fromVoice = true,
                                text = if (prefs.enableV3) lastState.fullReply else lastState.aiReplies.joinToString(" ") { it.text },
                                intent = lastState.intentName,
                                actionType = lastState.actionType,
                                attachments = lastState.attachments,
                            )
                        }
                        // Speak:
                        tts.speak(
                            aiReplies = lastState.aiReplies
                        )
                    }
                    audioRequestsManager.releaseDuckedFocus()
                    // Execute (only if end):
                    if (end && lastState.actionType != null) {
                        actionsExecutor.execute(lastState)
                    }

                } else if (voiceQueryOn) {
                    // B) First execute action, then speak:
                    // Execute (only if end):
                    if (end && lastState.actionType != null) {
                        newReplies = actionsExecutor.execute(lastState)
                        // Reset:
                        if (lastState.messageMode) {
                            lastState.messageMode = false
                            if (!prefs.enableV3) lastState.attachments.usable = null
                        }
                    }
                    // If received updated replies: replace!
                    if (newReplies.isNotEmpty())  lastState.aiReplies = newReplies

                    // Read:
                    if (lastState.aiReplies.isNotEmpty()) {
                        // Save reply:
                        if (!lastState.noSave) {
                            messageUtils.storeMessage(
                                context = applicationContext,
                                langCode = prefs.queryLanguage,
                                fromUser = false,
                                fromVoice = true,
                                text = if (prefs.enableV3) lastState.fullReply else lastState.aiReplies.joinToString(" ") { it.text },
                                intent = lastState.intentName,
                                actionType = lastState.actionType,
                                attachments = lastState.attachments,
                            )
                        }
                        // Speak:
                        tts.speak(
                            aiReplies = lastState.aiReplies,
                        )
                    }
                    audioRequestsManager.releaseDuckedFocus()

                } else {
                    stopSelf()
                }

                if (voiceQueryOn && (lastState.messageMode || followUp)) {
                    // START FOLLOWUP INTERACTION:
                    startVoiceRecording()

                } else {
                    // END:
                    queryStatus.postValue("ready")
                    if (lastState.fail || lastState.attachments.playFail) {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                    } else {
                        if (lastState.playAcknowledge || lastState.attachments.playAcknowledge) {
                            //ACKNOWLEDGE
                            toneGen.startTone(ToneGenerator.TONE_PROP_ACK)
                        }
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
