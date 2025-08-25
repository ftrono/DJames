package com.ftrono.DJames.application.services

import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ftrono.DJames.application.ACTION_FINISH_CLOCK
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.ACTION_REC_STOP
import com.ftrono.DJames.application.ACTION_SAVE_TRACK
import com.ftrono.DJames.application.ACTION_TIME_TICK
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.PHONE_STATE_ACTION
import com.ftrono.DJames.application.SPOTIFY_METADATA_CHANGED
import com.ftrono.DJames.application.VOLUME_CHANGED_ACTION
import com.ftrono.DJames.application.acts_active
import com.ftrono.DJames.application.audioManager
import com.ftrono.DJames.application.callMode
import com.ftrono.DJames.application.clickCounter
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recordingMode
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.application.streamMaxVolume
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.application.vol_initialized
import com.ftrono.DJames.ui.overlay.OverlayClose
import com.ftrono.DJames.ui.defaults.OverlayLifecycleOwner
import com.ftrono.DJames.ui.defaults.OverlaySavedStateRegistryOwner
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt
import com.ftrono.DJames.application.ACTION_OVERLAY_CLICK
import com.ftrono.DJames.application.allowVolumeClick
import com.ftrono.DJames.application.clickCountdownTime
import com.ftrono.DJames.application.clickSleepInterval
import com.ftrono.DJames.application.overlayOptionsStr
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.overlay.DJamesPads
import com.ftrono.DJames.ui.overlay.getQuickActionOnTap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay


class OverlayService : Service() {
    private val TAG = OverlayService::class.java.simpleName
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    //Compose Views:
    private lateinit var bubbleView : ComposeView
    private lateinit var closeView : ComposeView

    // Coroutine scope to handle countdown
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null
    private var loadJob: Job? = null
    private var saveTrackJob: Job? = null

    //View managers:
    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: OverlayLifecycleOwner
    private lateinit var savedStateRegistryOwner: OverlaySavedStateRegistryOwner
    private lateinit var bubbleParams: LayoutParams
    private lateinit var closeParams: LayoutParams

    //Mini Clock:
    private var currentTime = MutableLiveData<String>("00:00")
    private var now: LocalDateTime? = null
    private val miniClockFormat = DateTimeFormatter.ofPattern("HH:mm")

    //Vars:
    var screenHeight = 0
    var screenWidth = 0
    var restarting = false

    //Receiver:
    var eventReceiver = EventReceiver()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()
            vol_initialized = false
            callMode = false
            clickCounter.postValue(0)
            overlayActive.postValue(true)

            // Init window manager
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            // Store display height & width
            screenHeight = resources.displayMetrics.heightPixels
            screenWidth = resources.displayMetrics.widthPixels

            //Lifecycle owners:
            lifecycleOwner = OverlayLifecycleOwner()
            savedStateRegistryOwner = OverlaySavedStateRegistryOwner()

            //Layout flags:
            val LAYOUT_FLAGS = LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_KEEP_SCREEN_ON

            //1) BUBBLE VIEW:
            //Params:
            bubbleParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LAYOUT_FLAGS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = if (prefs.overlayPosition == "Right") screenWidth else 0
                y = round(screenHeight.toDouble()/3).toInt()
            }

            //Compose:
            bubbleView = ComposeView(this).also {
                it.setContent {
                    OverlayBubble(
                        centerSize = 100,
                        toeSize = 70,
                        onDrag = { x, y ->
                            bubbleParams.x += x
                            if (bubbleParams.y + y >= 0) {
                                bubbleParams.y += y
                            }
                            windowManager.updateViewLayout(it, bubbleParams)
                        }
                    )
                }
                it.setViewTreeLifecycleOwner(lifecycleOwner)
                it.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            }

            //Set current time:
            updateMiniClock()

            //Add the overlay view to the window
            windowManager.addView(bubbleView, bubbleParams)

            // Start the lifecycle
            lifecycleOwner.setCurrentState(Lifecycle.State.STARTED)
            // Initialize the SavedStateRegistry
            savedStateRegistryOwner.performRestore(null)


            //2) CLOSE VIEW:
            //Params:
            closeParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            //Specify the overlay view position
            closeParams.x = 0
            closeParams.y = screenHeight

            //Compose
            closeView = ComposeView(this).also {
                it.setContent {
                    OverlayClose()
                }
                it.setViewTreeLifecycleOwner(lifecycleOwner)
                it.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            }

            //RECEIVER:
            //Lower volume if maximum (to enable Receiver):
            if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume -1, AudioManager.FLAG_PLAY_SOUND)
                Log.d(TAG, "Overlay on: Volume lowered from Max.")
            }

            //Disable volume button press for the first 3 seconds:
            loadJob = serviceScope.launch {
                try {
                    delay(3000)
                    //Vol_initialized:
                    if (!vol_initialized) {
                        vol_initialized = true
                    }
                } catch (e: InterruptedException) {
                    Log.w(TAG, "Interrupted: exception.", e)
                }
            }

            //Start Event Receiver:
            val filter = IntentFilter()
            filter.addAction(VOLUME_CHANGED_ACTION)
            filter.addAction(SPOTIFY_METADATA_CHANGED)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(ACTION_TOASTER)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(eventReceiver, filter, RECEIVER_EXPORTED)
            Log.d(TAG, "EventReceiver started.")

            //Start personal Receiver:
            val actFilter = IntentFilter()
            actFilter.addAction(ACTION_TIME_TICK)
            actFilter.addAction(ACTION_OVERLAY_CLICK)
            actFilter.addAction(ACTION_SAVE_TRACK)
            actFilter.addAction(ACTION_MAKE_CALL)
            actFilter.addAction(PHONE_STATE_ACTION)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(overlayReceiver, actFilter, RECEIVER_EXPORTED)
            Log.d(TAG, "OverlayReceiver started.")

            //Set current time:
            updateMiniClock()


        } catch (e: Exception) {
            Log.w(TAG, "Overlay Service ERROR: ", e)
            stopSelf()
            //Redirect to App Permissions:
            Toast.makeText(
                applicationContext,
                "Please enable the 'Display over other apps' permission first!",
                Toast.LENGTH_LONG
            ).show()
            //Show Permissions page:
            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent1.putExtra("fromwhere", "ser")
            intent1.setData(uri)
            startActivity(intent1)
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Store display screenHeight & screenWidth
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        //Preferred xpos:
        bubbleParams.x = if (prefs.overlayPosition == "Right") screenWidth else 0
        bubbleParams.y = round(screenHeight.toDouble()/3).toInt()
        windowManager.updateViewLayout(bubbleView, bubbleParams)
    }


    @Composable
    fun OverlayBubble(
        centerSize: Int,
        toeSize: Int,
        onDrag: (Int, Int) -> Unit
    ) {
        // Coroutine scope for animating drag events
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
        val coroutineScope = rememberCoroutineScope()
        val clockActiveState by clockActive.observeAsState()
        val clickCounterState by clickCounter.observeAsState()
        val sourceIsVolumeState by sourceIsVolume.observeAsState()
        val overlayPosState by overlayPos.observeAsState()
        // Animating the horizontal offset based on the state
        val rightPadding by animateDpAsState(targetValue = if (
            clickCounterState!! > 0 && overlayPosState == "Right" && sourceIsVolumeState!!
        ) (70.dp) else 0.dp)


        //CONTAINER:
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(end = rightPadding)
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        //ON DRAG START:
                        onDragStart = {
                            if (clickCounterState == 0) {
                                //Add close view to the window:
                                showCloseView()
                            }
                        },
                        //ON DRAG:
                        onDrag = { _, dragAmount ->
                            if (clickCounterState == 0) {
                                onDrag(
                                    dragAmount.x.roundToInt(),
                                    dragAmount.y.roundToInt()
                                )
                            }
                        },
                        //ON DRAG END:
                        onDragEnd = {
                            if (clickCounterState == 0) {
                                // Hide close view:
                                var startClosingRegion =
                                    if (isLandscape) screenHeight * 0.5 else screenHeight * 0.7
                                removeCloseView()
                                // Check if overlay is in the lower 20% of the screen
                                if (abs(bubbleParams.y.toFloat()) >= (startClosingRegion)) {
                                    // If SWIPE DOWN -> CLOSE:
                                    stopSelf()
                                } else {
                                    //ANIMATE TO SCREEN EDGE:
                                    // Calculate target position:
                                    val animatable_X = Animatable(bubbleParams.x.toFloat())
                                    var targetX = 0f
                                    if (animatable_X.value > screenWidth / 2f - centerSize.dp.toPx() / 2) {
                                        //RIGHT:
                                        targetX = (screenWidth - centerSize.dp.toPx())
                                        overlayPos.postValue("Right")
                                        prefs.overlayPosition = "Right"
                                    } else {
                                        //LEFT:
                                        targetX = 0f
                                        overlayPos.postValue("Left")
                                        prefs.overlayPosition = "Left"
                                    }
                                    //Move:
                                    coroutineScope.launch {
                                        animatable_X.animateTo(
                                            targetValue = targetX,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = LinearOutSlowInEasing
                                            )
                                        ) {
                                            bubbleParams.x = value.toInt()
                                            windowManager.updateViewLayout(bubbleView, bubbleParams)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            DJamesPads(
                queryStatus = queryStatus,
                overlayPosState = overlayPosState!!,
                clickCounterState = clickCounterState!!,
                clockActiveState = clockActiveState!!,
                currentTime = currentTime,
                centerSize = centerSize,
                toeSize = toeSize,
                onToesTapCommon = {
                    onToesPadClick(fromVolume = false)
                },
                onCenterTap = {
                    if (!voiceQueryOn) {
                        // CENTER TAP:
                        onCenterPadClick(enable = clickCounterState == 0, fromVolume = false)
                    } else if (recordingMode) {
                        //EARLY STOP RECORDING:
                        Intent().also { intent ->
                            intent.setAction(ACTION_REC_STOP)
                            sendBroadcast(intent)
                        }
                    }
                }
            )
        }
    }


    //Animations:
    private fun fadeIn(view: View) {
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        fadeIn.duration = 500 // Duration in milliseconds
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.start()
    }


    private fun fadeOut(view: View) {
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        fadeOut.duration = 500 // Duration in milliseconds
        fadeOut.interpolator = DecelerateInterpolator()
        fadeOut.start()
    }


    //Show Close View:
    private fun showCloseView() {
        try {
            closeView.let {
                windowManager.addView(it, closeParams)
                fadeIn(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "CloseView: cannot show. ", e)
        }
    }


    //Remove Close View:
    private fun removeCloseView() {
        try {
            closeView.let {
                fadeOut(it)
                windowManager.removeView(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "CloseView: cannot remove. ")
        }
    }


    //Clock:
    fun updateMiniClock() {
        now = LocalDateTime.now()
        currentTime.postValue(now!!.format(miniClockFormat))
    }


    //Foreground Service:
    private fun startForeground() {
        //Foreground service:
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.DJames"
        val channelName = "Floating View Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)!!
        manager.createNotificationChannel(chan)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("DJames: Overlay Service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(1, notification)
    }

    fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    override fun onDestroy() {
        super.onDestroy()
        overlayActive.postValue(false)
        voiceQueryOn = false
        clickCounter.postValue(0)
        //Stop Voice Query service:
        if (isMyServiceRunning(VoiceQueryService::class.java)) {
            stopService(Intent(applicationContext, VoiceQueryService::class.java))
        }
        serviceScope.cancel() // Clean up coroutines
        try {
            saveTrackJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "SaveTrackJob not active.")
        }
        try {
            countdownJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "SaveTrackJob not active.")
        }
        vol_initialized = false
        //unregister receivers:
        try {
            unregisterReceiver(eventReceiver)
            Log.d(TAG, "eventReceiver stopped.")
        } catch (e: Exception) {
            Log.w(TAG, "eventReceiver: cannot unregister. ", e)
        }
        try {
            unregisterReceiver(overlayReceiver)
            Log.d(TAG, "overlayReceiver stopped.")
        } catch (e: Exception) {
            Log.w(TAG, "overlayReceiver: cannot unregister. ", e)
        }

        if (!restarting) {
            //End Clock Screen():
            Intent().also { intent ->
                intent.setAction(ACTION_FINISH_CLOCK)
                sendBroadcast(intent)
            }
        }
        try {
            bubbleView.let {
                windowManager.removeView(it)
                it.setViewTreeLifecycleOwner(null)
                it.setViewTreeSavedStateRegistryOwner(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "BubbleView: cannot remove. ", e)
        }
        try {
            closeView.let {
                windowManager.removeView(it)
                it.setViewTreeLifecycleOwner(null)
                it.setViewTreeSavedStateRegistryOwner(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "CloseView: cannot remove. ")
        }
        //If no activities active -> CLOSE APP:
        Log.d(TAG, "$acts_active")
        if (acts_active.size == 0) {
            System.exit(0)
        }
    }


    // COUNTDOWN FUNCTIONS:
    fun onToesPadClick(fromVolume: Boolean = false) {
        //CLICK -> Play ALERT tone:
        sourceIsVolume.postValue(fromVolume)
        restartCountdown()
        toneGen.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE)   //ALERT
    }

    fun onCenterPadClick(enable: Boolean, fromVolume: Boolean = false) {
        //CLICK -> Play ALERT tone:
        sourceIsVolume.postValue(fromVolume)
        restartCountdown()
        if (enable) {
            clickCounter.postValue(1)
            toneGen.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE)   //ALERT
        } else {
            clickCounter.postValue(0)
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
        }
    }

    fun loopPads(fromVolume: Boolean = false) {
        //CLICK -> Play ALERT tone:
        sourceIsVolume.postValue(fromVolume)
        restartCountdown()
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


    fun restartCountdown() {
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
                when (actionName) {
                    "speak" -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    "clock" -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    "save" -> toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER)   //STOP
                    "silence" -> toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                    else -> toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                }
                //TRIGGER ACTION:
                getQuickActionOnTap(applicationContext, actionName, toneGen)()
            }
            //Reset counter:
            clickCounter.postValue(0)
            sourceIsVolume.postValue(false)
            allowVolumeClick = true
            Log.d(TAG, "CountdownJob end!")
        }
    }

    //Restart service:
    fun restartService(context: Context) {
        //Play RESTART tone:
        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE)   //RESTART
        //RESTART:
        Log.d(TAG, "Restarting...")
        restarting = true
        stopSelf()
        if (!utils.isMyServiceRunning(OverlayService::class.java, context)) {
            try {
                var intentOS = Intent(context, OverlayService::class.java)
                context.startService(intentOS)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot auto-start Overlay Service. EXCEPTION: ", e)
            }
        }
    }


    //PERSONAL RECEIVER:
    private var overlayReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //Update clock (every minute):
            if (intent!!.action == ACTION_TIME_TICK) {
                updateMiniClock()
            }

            //Trigger overlay click:
            if (intent.action == ACTION_OVERLAY_CLICK) {
                loopPads(true)
            }

            //Save current track:
            if (intent.action == ACTION_SAVE_TRACK) {
                Log.d(TAG, "OVERLAY: ACTION_SAVE_TRACK.")
                try {
                    //PROCESS QUERY:
                    saveTrackJob = CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        spotifyUtils.saveCurrentTrack(context!!, toneGen)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ERROR: Cannot save current track! ", e)
                }
            }

            //MAKE A PHONE CALL:
            if (intent.action == ACTION_MAKE_CALL) {
                Log.d(TAG, "OVERLAY: ACTION_MAKE_CALL.")
                var toCall = intent.getStringExtra("toCall")
                callMode = true
                //MAKE CALL:
                val intentCall = Intent(Intent.ACTION_CALL)
                intentCall.setData(Uri.parse(toCall))
                intentCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intentCall.putExtra("fromwhere", "ser")
                startActivity(intentCall)
            }

            //Listen to phone state:
            if (intent.action == PHONE_STATE_ACTION) {
                Log.d(TAG, "EVENT: PHONE STATE CHANGED.")
                try {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    Log.d(TAG, "TELEPHONY INTENT STATE: $state")
                    if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        callMode = true
                        vol_initialized = false
                        Log.d(TAG, "EVENT: CALL MODE ON.")
                    } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                        callMode = false
                        //Private check thread:
                        try {
                            loadJob?.cancel()
                        } catch (e: Exception) {
                            Log.w(TAG, "loadJob not active.")
                        }
                        loadJob = serviceScope.launch {
                            try {
                                delay(3000)
                                //Vol_initialized:
                                if (!vol_initialized) {
                                    vol_initialized = true
                                }
                                restartService(context!!)
                                delay(1000)
                                restarting = false
                            } catch (e: InterruptedException) {
                                Log.w(TAG, "Interrupted: exception.", e)
                            }
                        }
                        Log.d(TAG, "EVENT: CALL MODE OFF.")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "EVENT: PHONE STATE CHANGED: receiver error. ", e)
                }
            }

        }
    }
}