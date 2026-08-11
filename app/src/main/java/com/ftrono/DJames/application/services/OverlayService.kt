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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.Observer
import com.ftrono.DJames.R
import com.ftrono.DJames.application.ACTION_FINISH_CLOCK
import com.ftrono.DJames.application.ACTION_MAKE_CALL
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
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.sourceIsVolume
import com.ftrono.DJames.application.streamMaxVolume
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.application.vol_initialized
import com.ftrono.DJames.ui.overlay.OverlayClose
import com.ftrono.DJames.ui.defaults.OverlayLifecycleOwner
import com.ftrono.DJames.ui.defaults.OverlaySavedStateRegistryOwner
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.ftrono.DJames.application.ACTION_OVERLAY_CLICK
import com.ftrono.DJames.application.ACTION_OVERLAY_HIDE
import com.ftrono.DJames.application.ACTION_OVERLAY_SHOW
import com.ftrono.DJames.application.App.Companion.toneGen
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.forceUndock
import com.ftrono.DJames.application.overlayBubbleSize
import com.ftrono.DJames.application.overlayToeSize
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.isVolumeUpUnlocked
import com.ftrono.DJames.application.mainActive
import com.ftrono.DJames.application.overlayDocked
import com.ftrono.DJames.ui.components.dpToPx
import com.ftrono.DJames.ui.overlay.Overlay
import com.ftrono.DJames.ui.overlay.OverlayBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlin.math.round


class OverlayService : Service () {
    private val TAG = this::class.java.simpleName

    //Compose Views:
    private lateinit var bubbleView : ComposeView
    private lateinit var closeView : ComposeView

    // Coroutine scope to handle countdown
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadJob: Job? = null
    private var saveTrackJob: Job? = null

    //View managers:
    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: OverlayLifecycleOwner
    private lateinit var savedStateRegistryOwner: OverlaySavedStateRegistryOwner
    private lateinit var bubbleParams: LayoutParams
    private lateinit var closeParams: LayoutParams

    //Overlay:
    private var overlay = Overlay()

    //Vars:
    var overlayViewOn = false
    var screenHeight = 0
    var screenWidth = 0
    var restarting = false

    //Receiver:
    var eventReceiver = EventReceiver()

    // Observers:
    private val clockActiveObserver = Observer<Boolean> {
        updateOverlayPosition()
    }
    private val mainActiveObserver = Observer<Boolean> {
        updateOverlayPosition()
    }
    private val forceUndockObserver = Observer<Boolean> {
        updateOverlayPosition()
    }

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
            val config = getResources().getConfiguration()

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
                x = 0
                y = 0
            }

            //Compose:
            bubbleView = ComposeView(this).also {
                it.setContent {
                    OverlayCaller(
                        context = applicationContext,
                        onDrag = { x, y ->
                            bubbleParams.x += x
                            if (bubbleParams.y + y >= 0) {
                                bubbleParams.y += y
                            }
                            windowManager.updateViewLayout(it, bubbleParams)
                        },
                    )
                }
                it.setViewTreeLifecycleOwner(lifecycleOwner)
                it.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            }

            //Set current time:
            overlay.updateMiniClock()

            // Add the overlay view to the window:
            showOverlayView()

            // Observe dock-state dependencies:
            clockActive.observeForever(clockActiveObserver)
            mainActive.observeForever(mainActiveObserver)
            forceUndock.observeForever(forceUndockObserver)

            // Make sure initial state is correct:
            updateOverlayPosition()

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
            actFilter.addAction(ACTION_OVERLAY_SHOW)
            actFilter.addAction(ACTION_OVERLAY_HIDE)
            actFilter.addAction(ACTION_OVERLAY_CLICK)
            actFilter.addAction(ACTION_SAVE_TRACK)
            actFilter.addAction(ACTION_MAKE_CALL)
            actFilter.addAction(PHONE_STATE_ACTION)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(overlayReceiver, actFilter, RECEIVER_EXPORTED)
            Log.d(TAG, "OverlayReceiver started.")

            //Set current time:
            overlay.updateMiniClock()


        } catch (e: Exception) {
            Log.w(TAG, "Overlay Service ERROR: ", e)
            stopSelf()
            Toast.makeText(
                applicationContext,
                "Cannot start overlay bubble!",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Store display screenHeight & screenWidth
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        updateOverlayPosition()
    }


    // Overlay pos calculator:
    private fun updateOverlayPosition() {
        if (!::bubbleParams.isInitialized) return
        if (!::bubbleView.isInitialized) return

        val isPortrait =
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val shouldDock =
            isPortrait && (clockActive.value == true || mainActive.value == true) && (forceUndock.value != true)

        overlayDocked.value = shouldDock

        if (shouldDock) {
            // DOCKED: bottom-center
            bubbleParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

            // CENTER_HORIZONTAL already centers the window,
            // so x must simply be zero.
            bubbleParams.x = 0

            // 0 = exactly against the bottom.
            // Use a positive value if you want some bottom margin.
            bubbleParams.y = 0

        } else {
            // FLOATING: left/right side
            bubbleParams.gravity = Gravity.TOP or Gravity.START

            val bubbleWidth =
                overlayBubbleSize.dpToPx(applicationContext)

            bubbleParams.x =
                if (prefs.overlayPosition == "Right") {
                    screenWidth - bubbleWidth
                } else 0

            bubbleParams.y =
                (screenHeight / 4f).roundToInt()
        }

        if (overlayViewOn) {
            windowManager.updateViewLayout(bubbleView, bubbleParams)
        }
    }


    @Composable
    fun OverlayCaller(
        context: Context,
        centerSize: Int = overlayBubbleSize,
        toeSize: Int = overlayToeSize,
        onDrag: (Int, Int) -> Unit
    ) {
        // Coroutine scope for animating drag events
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
        val coroutineScope = rememberCoroutineScope()
        val clickCounterState by clickCounter.observeAsState()
        val sourceIsVolumeState by sourceIsVolume.observeAsState()
        val overlayPosState by overlayPos.observeAsState()
        val overlayDockedState by overlayDocked.observeAsState(false)

        // Animating the horizontal offset based on the state
        val rightPadding by animateDpAsState(targetValue = if (
            clickCounterState!! > 0 && overlayPosState == "Right" && sourceIsVolumeState!!
        ) (70.dp) else 0.dp)

        OverlayBubble(
            context = context,
            overlay = overlay,
            centerSize = centerSize,
            toeSize = toeSize,
            modifier = Modifier
                .padding(end = rightPadding)
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        //ON DRAG START:
                        onDragStart = {
                            if (clickCounterState == 0 && !overlayDockedState) {
                                //Add close view to the window:
                                showCloseView()
                            }
                        },
                        //ON DRAG:
                        onDrag = { _, dragAmount ->
                            if (clickCounterState == 0 && !overlayDockedState) {
                                onDrag(
                                    dragAmount.x.roundToInt(),
                                    dragAmount.y.roundToInt()
                                )
                            }
                        },
                        //ON DRAG END:
                        onDragEnd = {
                            if (clickCounterState == 0 && !overlayDockedState) {
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
        )
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


    // Show overlay:
    fun showOverlayView() {
        if (!overlayViewOn) {
            windowManager.addView(bubbleView, bubbleParams)
            overlayViewOn = true
        }
    }


    // Remove views:
    fun removeOverlayView() {
        if (overlayViewOn) {
            try {
                bubbleView.let {
                    windowManager.removeView(it)
                    it.setViewTreeLifecycleOwner(null)
                    it.setViewTreeSavedStateRegistryOwner(null)
                }
                overlayViewOn = false
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
        }
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
            .setSmallIcon(R.drawable.app_icon_notification)
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
        // Remove observers:
        clockActive.removeObserver(clockActiveObserver)
        mainActive.removeObserver(mainActiveObserver)
        forceUndock.removeObserver(forceUndockObserver)
        // Re-enable volume-up trigger:
        clickCounter.postValue(0)
        isVolumeUpUnlocked.postValue(false)
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
            overlay.countdownJob?.cancel()
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
        removeOverlayView()
        //If no activities active -> CLOSE APP:
        Log.d(TAG, "$acts_active")
        if (acts_active.size == 0) {
            System.exit(0)
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
                overlay.updateMiniClock()
            }

            //Show overlay:
            if (intent.action == ACTION_OVERLAY_SHOW) {
                showOverlayView()
            }

            // Hide overlay:
            if (intent.action == ACTION_OVERLAY_HIDE) {
                removeOverlayView()
            }

            //Trigger overlay click:
            if (intent.action == ACTION_OVERLAY_CLICK) {
                overlay.loopPads(applicationContext, true)
            }

            //Save current track:
            if (intent.action == ACTION_SAVE_TRACK) {
                Log.d(TAG, "OVERLAY: ACTION_SAVE_TRACK.")
                try {
                    //PROCESS QUERY:
                    saveTrackJob = CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        spotifyUtils.saveCurrentTrack(context!!)
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