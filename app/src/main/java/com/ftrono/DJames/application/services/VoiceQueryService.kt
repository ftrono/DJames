package com.ftrono.DJames.application.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.logUtils
import com.ftrono.DJames.application.overlayStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.application.recordingFail
import com.ftrono.DJames.application.recordingTime
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.NLPDispatcher
import com.ftrono.DJames.be.audio.AndroidAudioRecorder
import com.ftrono.DJames.be.audio.AudioRequestsManager
import com.ftrono.DJames.be.audio.TTSReader
import com.ftrono.DJames.be.tools.Actions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File


class VoiceQueryService: Service() {

    //Main:
    private val TAG = VoiceQueryService::class.java.simpleName
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val audioRequestsManager = AudioRequestsManager()
    private lateinit var tts: TTSReader

    //Recorder:
    private val MyRecorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    //Status:
    private var followUp = false
    private var messageMode = false
    private var dispatcherInfo = DispatcherInfo()

    //JOBS:
    private var recordingJob: Job? = null
    private var processingJob: Job? = null

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
        cancelJob(recordingJob, "recordingJob")
        cancelJob(processingJob, "processingJob")

        //Stop recorder:
        if (recordingMode || overlayStatus.value != "ready") {
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
        dispatcherInfo = DispatcherInfo()

        //Set overlay READY color:
        overlayStatus.postValue("ready")
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


    fun cancelJob(job: Job?, jobName: String) {
        try {
            job?.cancel()
            Log.d(TAG, "Stopped $jobName!")
        } catch (e: Exception) {
            Log.w(TAG, "$jobName not active.")
        }
    }

    private fun failSilently() {
        toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        stopSelf()
    }


    // INTERACTION FLOW:
    // START RECORDING:
    private fun startVoiceRecording(
        speakIntro: Boolean = false
    ) {
        Log.d(TAG, "startVoiceRecording() triggered!")
        recordingTime = 0
        try {
            cancelJob(processingJob, "processingJob")
            //Start rec Job:
            recordingJob = coroutineScope.launch {
                // 1) SPEAK INTRO:
                if (voiceQueryOn && speakIntro) {
                    audioRequestsManager.requestDuckedFocus(
                        onGranted = {
                            overlayStatus.postValue("processing")
                            Thread.sleep(500)
                            //Read TTS:
                            tts.speak(
                                listOf(
                                    AiReply(
                                        langCode = prefs.queryLanguage,
                                        text = defaultReplies.speakIntro()
                                    )
                                )
                            )
                        },
                        onFail = { failSilently() }
                    )
                    audioRequestsManager.releaseDuckedFocus()
                }

                // 2) RECORD:
                if (voiceQueryOn) {
                    audioRequestsManager.requestExclusiveFocus(
                        onGranted = {
                            //Set overlay BUSY color:
                            overlayStatus.postValue("busy")

                            //Play START tone:
                            toneGen.startTone(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE)   //START
                            // toneGen.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP)   //FOLLOW UP

                            //Start recording (default: cacheDir):
                            recordingMode = true
                            MyRecorder.start()

                            MyRecorder.whileRecording(
                                messageMode,
                                dispatcherInfo.messageType,
                            )
                        },
                        onFail = { failSilently() }
                    )
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "VQSERVICE: EXCEPTION: ", e)
            failSilently()
        }
    }


    // STOP RECORDING:
    fun stopVoiceRecording() {
        Log.d(TAG, "stopVoiceRecording() triggered")
        try {
            var recFile = MyRecorder.stop()
            recordingMode = false
            Log.d(TAG, "RECORDING STOPPED.")
            cancelJob(recordingJob, "recordingJob")


            //2) RECORDING RESULT:
            if (!voiceQueryOn || recordingFail) {
                //A) RECORDING FAIL -> END:
                failSilently()

            } else {
                //B) RECORDING SUCCESS:
                //Play STOP tone:
                toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP
                audioRequestsManager.releaseExclusiveFocus()
                audioRequestsManager.requestDuckedFocus(
                    onGranted = {
                        if (voiceQueryOn) {
                            //Set overlay PROCESSING color & icon:
                            overlayStatus.postValue("processing")
                            //PROCESS QUERY:
                            processingJob = coroutineScope.launch {
                                processQuery(recFile)
                            }
                        }
                    },
                    onFail = { failSilently() }
                )

            }

        } catch (e: Exception) {
            Log.w(TAG, "VQSERVICE: EXCEPTION: ", e)
            failSilently()
        }
    }


    // PROCESS QUERY:
    private fun processQuery(recFile: File) {
        Log.d(TAG, "PROCESSING JOB STARTED!")
        try {
            //PROCESS REQUEST:
            var nlpDispatcher = NLPDispatcher(applicationContext)
            dispatcherInfo = nlpDispatcher.dispatch(recFile, dispatcherInfo, followUp, messageMode)
            messageMode = dispatcherInfo.messageMode
            followUp = dispatcherInfo.followUp
            val actions = Actions(applicationContext)
            var newReplies = listOf<AiReply>()

            if (dispatcherInfo.fail && dispatcherInfo.aiReplies.isEmpty()) {
                // Default fail replies:
                newReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = defaultReplies.replyError()
                    )
                )
            }

            // Speak & execute:
            val intentName = lastLog.nlpQueries.first().intentName
            if (intentName.contains("Play") || intentName.contains("Call")) {
                // A) First speak, then execute action:
                // Read:
                if (dispatcherInfo.aiReplies.isNotEmpty()) {
                    tts.speak(dispatcherInfo.aiReplies)
                }
                audioRequestsManager.releaseDuckedFocus()
                // Execute:
                if (dispatcherInfo.actionType != null) {
                    actions.execute(dispatcherInfo)
                }

            } else {
                // B) First execute action, then speak:
                // Execute:
                if (dispatcherInfo.actionType != null) {
                    newReplies = actions.execute(dispatcherInfo)
                }
                // If received updated replies: replace!
                if (newReplies.isNotEmpty()) {
                    dispatcherInfo.aiReplies = newReplies
                }
                // Read:
                if (dispatcherInfo.aiReplies.isNotEmpty()) {
                    tts.speak(dispatcherInfo.aiReplies)
                }
                audioRequestsManager.releaseDuckedFocus()
            }

            if (messageMode || followUp) {
                // START FOLLOWUP INTERACTION:
                startVoiceRecording()

            } else {
                // END:
                overlayStatus.postValue("ready")
                Thread.sleep(200)
                if (dispatcherInfo.fail) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                } else {
                    if (dispatcherInfo.end) logUtils.storeLog(applicationContext)   //Close log
                    if (dispatcherInfo.playAcknowledge) toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                }
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "VQSERVICE: EXCEPTION: ", e)
            failSilently()
        }
    }


    //PERSONAL RECEIVER:
    private var VQReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Stop Recording:
            if (intent!!.action == ACTION_REC_STOP && recordingMode && recordingTime >= 1) {
                Log.d(TAG, "VQRECEIVER: ACTION_REC_STOP.")
                stopVoiceRecording()
            }

        }
    }

}
