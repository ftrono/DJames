package com.ftrono.DJames.services

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import kotlin.math.abs
import kotlin.math.round


class FloatingViewService : Service() {
    private val TAG = FloatingViewService::class.java.simpleName

    //Pos:
    var height = 0
    var width = 0
    var xpos = 0

    //View managers:
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null
    private var mCloseView: View? = null
    private var params: LayoutParams? = null
    private var params2: LayoutParams? = null

    //Receiver:
    var eventReceiver = EventReceiver()

    //Overlay resources:
    private var overlayButton: View? = null   //eventReceiver(clock opened, clock closed), VoiceQueryService(setOverlay ready/busy/processing)
    private var overlayIcon: ImageView? = null   //eventReceiver(clock opened, clock closed), VoiceQueryService(setOverlay ready/busy/processing)
    private var overlayClockButton: View? = null   //eventReceiver(clock opened, clock closed)
    private var overlayClockIcon: ImageView? = null   //eventReceiver(clock opened, clock closed)
    private var overlayClockText: TextView? = null   //eventReceiver(clock opened, clock closed)

    //Disable volume button press for the first 3 seconds:
    val loadThread = Thread {
        try {
            synchronized(this) {
                Thread.sleep(3000)
                //Vol_initialized:
                if (!vol_initialized) {
                    vol_initialized = true
                }
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()
            vol_initialized = false
            callMode = false
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_OVERLAY_ACTIVATED)
                sendBroadcast(intent)
            }
            overlay_active = true

            //RECEIVER:
            //Lower volume if maximum (to enable Receiver):
            if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume-1, AudioManager.FLAG_PLAY_SOUND)
                Log.d(TAG, "Overlay on: Volume lowered from Max.")
            }

            //Thread check:
            if (!loadThread.isAlive()){
                loadThread.start()
            }

            //Start Event Receiver:
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(VOLUME_CHANGED_ACTION)
            filter.addAction(ACTION_TOASTER)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(eventReceiver, filter, RECEIVER_EXPORTED)
            Log.d(TAG, "Receiver started.")

            //Start personal Receiver:
            val actFilter = IntentFilter()
            actFilter.addAction(ACTION_CLOCK_OPENED)
            actFilter.addAction(ACTION_CLOCK_CLOSED)
            actFilter.addAction(ACTION_OVERLAY_READY)
            actFilter.addAction(ACTION_OVERLAY_BUSY)
            actFilter.addAction(ACTION_OVERLAY_PROCESSING)
            actFilter.addAction(ACTION_MAKE_CALL)
            actFilter.addAction(PHONE_STATE_ACTION)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(overlayReceiver, actFilter, RECEIVER_EXPORTED)
            Log.d(TAG, "OverlayReceiver started.")


            //VIEW:
            // Init window manager
            mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager?

            // Store display height & width
            height = resources.displayMetrics.heightPixels
            width = resources.displayMetrics.widthPixels

            //Preferred xpos:
            if (prefs.overlayPosition.toInt() == 1) {
                //RIGHT:
                xpos = width
            } else {
                //LEFT:
                xpos = -width
            }

            //Layout flags:
            val LAYOUT_FLAGS = LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_KEEP_SCREEN_ON

            // OVERLAY BUTTON:
            //Inflate the overlay view layout we created
            mFloatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LAYOUT_FLAGS,
                PixelFormat.TRANSLUCENT
            )

            //Specify the overlay view position
            params!!.gravity =
                Gravity.TOP or Gravity.LEFT //Initially view will be added to top-left corner
            params!!.x = xpos
            params!!.y = round(height.toDouble()/3).toInt()

            //Add the overlay view to the window
            mWindowManager!!.addView(mFloatingView, params)

            // CLOSE TEXT:
            //Inflate close layout
            mCloseView = LayoutInflater.from(this).inflate(R.layout.close_layout, null)
            params2 = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            //Specify the overlay view position
            params2!!.x = xpos
            params2!!.y = height

            // Set the overlay button & icon
            overlayButton =
                mFloatingView!!.findViewById<View>(R.id.rounded_button)
            overlayIcon = mFloatingView!!.findViewById<ImageView>(R.id.record_icon)

            //Set the overlay Clock button
            overlayClockButton = mFloatingView!!.findViewById<View>(R.id.clock_button)
            overlayClockIcon = mFloatingView!!.findViewById<ImageView>(R.id.clock_icon)
            overlayClockText = mFloatingView!!.findViewById<TextView>(R.id.clock_desc)

            overlayClockButton!!.setOnClickListener {

                if (!clock_active) {
                    //Start fake lock screen:
                    val intent1 = Intent(this, FakeLockScreen::class.java)
                    intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent1.putExtra("fromwhere", "ser")
                    startActivity(intent1)

                    //End Main():
                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_FINISH_MAIN)
                        sendBroadcast(intent)
                    }

                    //End Settings():
                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_FINISH_SETTINGS)
                        sendBroadcast(intent)
                    }

                }
            }

            if (clock_active) {
                overlayClockButton!!.visibility = View.INVISIBLE
                overlayClockIcon!!.visibility = View.INVISIBLE
                overlayClockText!!.visibility = View.INVISIBLE
            }

            //VIEW EVENTS HANDLER:
            mFloatingView!!.findViewById<View>(R.id.root_container)
                .setOnTouchListener(object : OnTouchListener {
                    private var initialX = 0
                    private var initialY = 0
                    private var initialTouchX = 0f
                    private var initialTouchY = 0f
                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        when (event.action) {
                            //Finger on screen:
                            MotionEvent.ACTION_DOWN -> {

                                //Add the close view to the window
                                mWindowManager!!.addView(mCloseView, params2)

                                //remember the initial position.
                                initialX = params!!.x
                                initialY = params!!.y

                                //get the touch location
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY

                                return true
                            }
                            //Finger lifted:
                            MotionEvent.ACTION_UP -> {
                                val Xdiff = (event.rawX - initialTouchX).toInt()
                                val Ydiff = (event.rawY - initialTouchY).toInt()

                                //The check for abs(Xdiff) < 10 && abs(YDiff) < 10 is because sometime elements moves a little while clicking.
                                //So that is click event.
                                // ON CLICK:
                                if (abs(Xdiff) < 10 && abs(Ydiff) < 10) {
                                    if (!voiceQueryOn) {
                                        //START VOICE QUERY SERVICE:
                                        sourceIsVolume = false
                                        try {
                                            startService(Intent(applicationContext, VoiceQueryService::class.java))
                                            Log.d(TAG, "OVERLAY SERVICE: VOICE QUERY SERVICE CALLED.")
                                        } catch (e:Exception) {
                                            Log.d(TAG, "OVERLAY SERVICE ERROR: VOICE QUERY SERVICE NOT STARTED. ", e)
                                        }
                                    } else if (recordingMode) {
                                        //EARLY STOP RECORDING:
                                        Intent().also { intent ->
                                            intent.setAction(ACTION_REC_STOP)
                                            sendBroadcast(intent)
                                        }
                                    }
                                } else if (abs(event.rawY) >= (height - 200)) {
                                    // If SWIPE DOWN -> CLOSE:
                                    // Log.d(FloatingViewService.TAG, "Current location: " + event.rawX + " / " + halfwidth + ", " + event.rawY + " / " + height)
                                    stopSelf()
                                }
                                if (mCloseView != null) mWindowManager!!.removeView(mCloseView)
                                return true
                            }
                            //Finger moved (item dragged):
                            MotionEvent.ACTION_MOVE -> {

                                //Calculate the X and Y coordinates of the view.
                                //params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                                params!!.y = initialY + (event.rawY - initialTouchY).toInt()

                                //Update the layout with new X & Y coordinate
                                mWindowManager!!.updateViewLayout(mFloatingView, params)
                                return true
                            }
                        }
                        return false
                    }
                })
        } catch (e: Exception) {
            Log.d(TAG, "Exception: ", e)
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_OVERLAY_DEACTIVATED)
                sendBroadcast(intent)
            }
            overlay_active = false
            voiceQueryOn = false
            //Stop Voice Query service:
            if (isMyServiceRunning(VoiceQueryService::class.java)) {
                stopService(Intent(applicationContext, VoiceQueryService::class.java))
            }
            overlayButton = null
            overlayIcon = null
            vol_initialized = false
            //unregister receivers:
            unregisterReceiver(eventReceiver)
            unregisterReceiver(overlayReceiver)
            Log.d(TAG, "Receivers stopped.")
            //End Fake Lock Screen():
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_FINISH_CLOCK)
                sendBroadcast(intent)
            }
            Toast.makeText(
                applicationContext,
                getString(R.string.str_enable_overlay),
                Toast.LENGTH_LONG
            ).show()
            //Show permissions page:
            val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent1.putExtra("fromwhere", "ser")
            intent1.setData(uri)
            startActivity(intent1)
            stopSelf()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Store display height & width
        width = resources.displayMetrics.widthPixels
        height = resources.displayMetrics.heightPixels
        //Preferred xpos:
        if (prefs.overlayPosition.toInt() == 1) {
            //RIGHT:
            xpos = width
        } else {
            //LEFT:
            xpos = -width
        }
        params!!.x = xpos
        params!!.y = round(height.toDouble()/3).toInt()
        mWindowManager!!.updateViewLayout(mFloatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        var processName = Application.getProcessName();
        Log.d(TAG, "Current process: " + processName)
        if (mFloatingView != null) mWindowManager!!.removeView(mFloatingView)
        voiceQueryOn = false
        //Stop Voice Query service:
        if (isMyServiceRunning(VoiceQueryService::class.java)) {
            stopService(Intent(applicationContext, VoiceQueryService::class.java))
        }
        if (loggedIn) {
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_OVERLAY_DEACTIVATED)
                sendBroadcast(intent)
            }
        }
        //End Fake Lock Screen():
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_FINISH_CLOCK)
            sendBroadcast(intent)
        }
        overlay_active = false
        //unregister receivers:
        unregisterReceiver(eventReceiver)
        unregisterReceiver(overlayReceiver)
        Log.d(TAG, "Receivers stopped.")
        //reset views:
        overlayButton = null
        overlayIcon = null
        overlayClockButton = null
        overlayClockIcon = null
        overlayClockText = null
        vol_initialized = false
        //If no activities active -> CLOSE APP:
        Log.d(TAG, "$acts_active")
        if (acts_active.size == 0) {
            System.exit(0)
        }
    }



    private fun startForeground() {
        //Foreground service:
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.DJames"
        val channelName = "Floating View Service"
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
            .setContentTitle("DJames: Floating View Service is running in background")
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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        var faded = intent.getBooleanExtra("faded", false)
        if (faded) {
            setClockOpened()
        } else {
            setClockClosed()
        }

        return START_NOT_STICKY
    }

    fun setClockOpened() {
        overlayButton!!.setBackgroundResource(R.drawable.rounded_button_dark)
        overlayIcon!!.setImageResource(R.drawable.speak_icon_gray)
        overlayClockButton!!.visibility = View.INVISIBLE
        overlayClockIcon!!.visibility = View.INVISIBLE
        overlayClockText!!.visibility = View.INVISIBLE
    }

    fun setClockClosed() {
        overlayButton!!.setBackgroundResource(R.drawable.rounded_button_ready)
        overlayIcon!!.setImageResource(R.drawable.speak_icon)
        overlayClockButton!!.visibility = View.VISIBLE
        overlayClockIcon!!.visibility = View.VISIBLE
        overlayClockText!!.visibility = View.VISIBLE
    }


    //PERSONAL RECEIVER:
    private var overlayReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //When Fake Lock Screen is opened:
            if (intent!!.action == ACTION_CLOCK_OPENED) {
                Log.d(TAG, "OVERLAY: ACTION_CLOCK_OPENED.")
                if (!voiceQueryOn) {
                    try {
                        setClockOpened()
                    } catch (e: Exception) {
                        Log.d(TAG, "OVERLAY: ACTION_CLOCK_OPENED: resources not available.")
                    }
                }
            }

            //When Fake Lock Screen is closed:
            if (intent.action == ACTION_CLOCK_CLOSED) {
                Log.d(TAG, "OVERLAY: ACTION_CLOCK_CLOSED.")
                if (!voiceQueryOn) {
                    try {
                        setClockClosed()
                    } catch (e: Exception) {
                        Log.d(TAG, "OVERLAY: ACTION_CLOCK_CLOSED: resources not available.")
                    }
                }
            }

            //Set overlay ready:
            if (intent.action == ACTION_OVERLAY_READY) {
                Log.d(TAG, "OVERLAY: ACTION_OVERLAY_READY.")
                try {
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
                } catch (e: Exception) {
                    Log.d(TAG, "OVERLAY: ACTION_OVERLAY_READY: resources not available.")
                }
            }

            //Set overlay busy:
            if (intent.action == ACTION_OVERLAY_BUSY) {
                Log.d(TAG, "OVERLAY: ACTION_OVERLAY_BUSY.")
                try {
                    //Set overlay BUSY color:
                    if (screenOn && overlayButton != null) {
                        overlayButton!!.setBackgroundResource(R.drawable.rounded_button_busy)
                        overlayIcon!!.setImageResource(R.drawable.speak_icon)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "OVERLAY: ACTION_OVERLAY_BUSY: resources not available.")
                }
            }

            //Set overlay processing:
            if (intent.action == ACTION_OVERLAY_PROCESSING) {
                Log.d(TAG, "OVERLAY: ACTION_OVERLAY_PROCESSING.")
                try {
                    //Set overlay PROCESSING color & icon:
                    if (screenOn && overlayButton != null && overlayIcon != null) {
                        overlayButton!!.setBackgroundResource(R.drawable.rounded_button_processing)
                        overlayIcon!!.setImageResource(R.drawable.looking_icon)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "OVERLAY: ACTION_OVERLAY_PROCESSING: resources not available.")
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
                    }
                    else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                        callMode = false
                        //Private check thread:
                        val loadThread2 = Thread {
                            try {
                                synchronized(this) {
                                    Thread.sleep(3000)
                                    //Vol_initialized:
                                    if (!vol_initialized) {
                                        vol_initialized = true
                                    }
                                }
                            } catch (e: InterruptedException) {
                                Log.d(TAG, "Interrupted: exception.", e)
                            }
                        }
                        //Thread check:
                        if (!loadThread2.isAlive()){
                            loadThread2.start()
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