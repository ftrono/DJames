package com.ftrono.DJames.ui.overlay

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.App.Companion.toneGen
import com.ftrono.DJames.application.allowVolumeClick
import com.ftrono.DJames.application.clickCountdownTime
import com.ftrono.DJames.application.clickCounter
import com.ftrono.DJames.application.clickSleepInterval
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.currentTime
import com.ftrono.DJames.application.isVolumeUpUnlocked
import com.ftrono.DJames.application.overlayBubbleSize
import com.ftrono.DJames.application.overlayOptionsStr
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.overlayToeSize
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.application.voiceQueryOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// Main OverlayBubble UI wrapper:
@Composable
fun OverlayBubble(
    context: Context,
    overlay: Overlay,
    centerSize: Int = overlayBubbleSize,
    toeSize: Int = overlayToeSize,
    modifier: Modifier,
) {
    // States:
    val clockActiveState by clockActive.observeAsState()
    val clickCounterState by clickCounter.observeAsState()
    val isVolumeUpUnlockedState by isVolumeUpUnlocked.observeAsState()
    val overlayPosState by overlayPos.observeAsState()

    //CONTAINER:
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // MAIN:
        DJamesPads(
            context = context,
            queryStatus = queryStatus,
            overlayPosState = overlayPosState!!,
            clickCounterState = clickCounterState!!,
            clockActiveState = clockActiveState!!,
            centerSize = centerSize,
            toeSize = toeSize,
            onToesTapCommon = {
                overlay.onToesPadClick(context)
            },
            onCenterTap = {
                if (isVolumeUpUnlockedState!!) {
                    // Re-enable volume-up trigger:
                    isVolumeUpUnlocked.postValue(false)
                } else {
                    if (!voiceQueryOn) {
                        // CENTER TAP:
                        overlay.onCenterPadClick(
                            context = context,
                            enable = clickCounterState == 0,
                        )
                    } else if (recordingMode) {
                        //EARLY STOP RECORDING:
                        Intent().also { intent ->
                            intent.setAction(ACTION_REC_STOP)
                            context.sendBroadcast(intent)
                        }
                    }
                }
            }
        )
    }
}


// Main Overlay class, with all needed Overlay methods:
class Overlay {
    private val TAG = this::class.java.simpleName
    var countdownJob: Job? = null

    //Mini Clock:
    private var now: LocalDateTime? = null
    private val miniClockFormat = DateTimeFormatter.ofPattern("HH:mm")


    //Clock:
    fun updateMiniClock() {
        now = LocalDateTime.now()
        currentTime.postValue(now!!.format(miniClockFormat))
    }

    // COUNTDOWN FUNCTIONS:
    fun onToesPadClick(context: Context) {
        //CLICK ONLY -> Play ALERT tone:
        sourceIsVolume.postValue(false)
        restartCountdown(context)
        toneGen.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE)   //ALERT
    }

    fun onCenterPadClick(context: Context, enable: Boolean) {
        //CLICK ONLY -> Play ALERT tone:
        sourceIsVolume.postValue(false)
        restartCountdown(context)
        if (enable) {
            clickCounter.postValue(1)
            toneGen.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE)   //ALERT
        } else {
            clickCounter.postValue(0)
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        }
    }

    fun loopPads(context: Context, fromVolume: Boolean = false) {
        //VOLUME UP ONLY -> Play ALERT tone:
        sourceIsVolume.postValue(fromVolume)
        restartCountdown(context)
        val maxClickOptions = 1 + overlayOptionsStr.value!!.split(", ").size
        if (clickCounter.value!! == maxClickOptions) {
            clickCounter.postValue(0)
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        } else {
            clickCounter.postValue(clickCounter.value!! + 1)
            toneGen.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE)   //ALERT
        }
    }


    fun restartCountdown(context: Context) {
        countdownJob?.cancel() // Cancel any running countdown
        Log.d(TAG, "CountdownJob canceled!")

        // THREAD:
        countdownJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "CountdownJob start!")
            //Countdown: ensure interval between clicks:
            delay(clickSleepInterval)
            if (!allowVolumeClick) {
                allowVolumeClick = true
            }
            delay(clickCountdownTime-clickSleepInterval)
            //After countdown:
            val overlayOptions = overlayOptionsStr.value!!.split(", ")
            var actionName = ""

            if (clickCounter.value!! == 1) {
                //Play FAIL tone:
                toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL

            } else if (clickCounter.value!! > 1) {
                val actionIndex = clickCounter.value!! - 2
                actionName = overlayOptions[actionIndex]
                when {
                    (actionName == "speak" && prefs.enableIntro) -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    (actionName == "speak") -> { }   // Do nothing
                    (actionName == "clock") -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    (actionName == "volume") -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    (actionName == "save") -> toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP
                    else -> toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                }
                //TRIGGER ACTION:
                getQuickActionOnTap(context, actionName)()
            }

            //Reset counter:
            clickCounter.postValue(0)
            sourceIsVolume.postValue(false)
            allowVolumeClick = true
            Log.d(TAG, "CountdownJob end!")
        }
    }
}