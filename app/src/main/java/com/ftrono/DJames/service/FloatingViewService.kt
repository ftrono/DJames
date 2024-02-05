package com.ftrono.DJames.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import com.ftrono.DJames.receivers.EventReceiver
import kotlin.math.abs
import kotlin.math.round


class FloatingViewService : Service() {
    private val TAG = FloatingViewService::class.java.simpleName
    private var fs_active : Boolean = false

    //View managers:
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null
    private var mCloseView: View? = null
    private var params: LayoutParams? = null
    private var params2: LayoutParams? = null

    //Receiver:
    var eventReceiver = EventReceiver()

    //Service status checker:
    val volumeThread = Thread {
        try {
            while (fs_active) {
                synchronized(this) {
                    //Log.d(TAG, "Overlay Service: volumeThread alive.")
                    try {
                        Thread.sleep(2000)
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "Overlay Service: volumeThread already stopped.")
                    }
                    //Lower volume if maximum (to enable Receiver):
                    if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                        audioManager!!.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                        Log.d(TAG, "Overlay Volume Check: Volume lowered.")
                    }
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
            fs_active = true

            //RECEIVER:
            //Prepare volume for Receiver:
            audioManager!!.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
            Log.d(TAG, "Volume lowered.")

            //Thread check:
            if (!volumeThread.isAlive()){
                volumeThread.start()
            }

            //Start Receiver:
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction("android.media.VOLUME_CHANGED_ACTION")
            filter.addAction(ACTION_CLOCK_OPENED)
            filter.addAction(ACTION_CLOCK_CLOSED)

            //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
            registerReceiver(eventReceiver, filter, RECEIVER_EXPORTED)
            Log.d(TAG, "Receiver started.")

            //VIEW:
            // Init window manager
            mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager?
            // Store display height & width
            var height = resources.displayMetrics.heightPixels
            var width = resources.displayMetrics.widthPixels
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
            params!!.x = width
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
                LAYOUT_FLAGS,
                PixelFormat.TRANSLUCENT
            )

            //Specify the overlay view position
            params2!!.x = width
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
                    intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
                    intent1.putExtra("fromwhere", "ser")
                    startActivity(intent1)
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
                                height = resources.displayMetrics.heightPixels
                                width = resources.displayMetrics.widthPixels

                                //The check for abs(Xdiff) < 10 && abs(YDiff) < 10 is because sometime elements moves a little while clicking.
                                //So that is click event.
                                // ON CLICK:
                                if (abs(Xdiff) < 10 && abs(Ydiff) < 10) {
                                    if (!recordingMode) {
                                        //START VOICE SEARCH SERVICE:
                                        recordingMode = true
                                        sourceIsVolume = false
                                        startService(Intent(applicationContext, VoiceSearchService::class.java))
                                    }
                                } else if (abs(event.rawY) >= (height - 200)) {
                                    // If SWIPE DOWN -> CLOSE:
                                    // Log.d(FloatingViewService.TAG, "Current location: " + event.rawX + " / " + halfwidth + ", " + event.rawY + " / " + height)
                                    //Send broadcast:
                                    Intent().also { intent ->
                                        intent.setAction(ACTION_OVERLAY_DEACTIVATED)
                                        sendBroadcast(intent)
                                    }
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
            recordingMode = false
            fs_active = false
            overlayButton = null
            overlayIcon = null
            //unregister receivers:
            unregisterReceiver(eventReceiver)
            Log.d(TAG, "Receiver stopped.")
            Toast.makeText(
                applicationContext,
                getString(R.string.str_enable_overlay),
                Toast.LENGTH_LONG
            ).show()
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
        var newWidth = resources.displayMetrics.widthPixels
        var newHeight = resources.displayMetrics.heightPixels
        params!!.x = newWidth
        params!!.y = round(newHeight.toDouble()/3).toInt()
        mWindowManager!!.updateViewLayout(mFloatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mFloatingView != null) mWindowManager!!.removeView(mFloatingView)
            fs_active = false
            recordingMode = false
            //unregister receivers:
            unregisterReceiver(eventReceiver)
            Log.d(TAG, "Receiver stopped.")
            //reset views:
            overlayButton = null
            overlayIcon = null
            overlayClockButton = null
            overlayClockIcon = null
            overlayClockText = null
            //Thread check:
            if (volumeThread.isAlive()){
                volumeThread.interrupt()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Interrupted: exception.", e)
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
}