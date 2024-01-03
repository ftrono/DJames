package com.ftrono.djeenoforspotify.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.app.NotificationCompat
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.application.MainActivity
import com.ftrono.djeenoforspotify.application.SettingsActivity
import kotlin.math.abs


class FloatingViewService : Service() {
    private val TAG = MainActivity::class.java.simpleName
    //View managers:
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null
    private var mCloseView: View? = null
    private var params: LayoutParams? = null
    private var params2: LayoutParams? = null
    //Shared preferences:
    private var navEnabled : Boolean = false
    private var valRecTimeout : String? = null
    private var valMapsTimeout : String? = null
    private var spotifyToken : String? = null
    private var mapsAddress : String? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        startMyOwnForeground()

        // Load preferences:
        val sharedPrefs = applicationContext.getSharedPreferences(SettingsActivity.SETTINGS_STORAGE, MODE_PRIVATE)

        // Init window manager
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager?
        // Store display height & width
        var height = resources.displayMetrics.heightPixels
        var halfwidth = resources.displayMetrics.widthPixels

        // OVERLAY BUTTON:
        //Inflate the overlay view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        val LAYOUT_FLAG = LayoutParams.TYPE_APPLICATION_OVERLAY
        params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the overlay view position
        params!!.gravity =
            Gravity.TOP or Gravity.LEFT //Initially view will be added to top-left corner
        params!!.x = 0
        params!!.y = 100

        //Add the overlay view to the window
        mWindowManager!!.addView(mFloatingView, params)

        // CLOSE TEXT:
        //Inflate close layout
        mCloseView = LayoutInflater.from(this).inflate(R.layout.close_layout, null)
        val LAYOUT_FLAG2 = LayoutParams.TYPE_APPLICATION_OVERLAY
        params2 = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG2,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the overlay view position
        params2!!.x = 0
        params2!!.y = height

        // Set the overlay button & icon
        val overlayButton = mFloatingView!!.findViewById<View>(R.id.rounded_button) as RelativeLayout
        val overlayIcon = mFloatingView!!.findViewById<View>(R.id.record_icon) as ImageView

        /*
        //Set the close button
        val closeButtonCollapsed = mFloatingView!!.findViewById<View>(R.id.close_btn) as ImageView
        closeButtonCollapsed.setOnClickListener { //close the service and remove the from from the window
            stopSelf()
        }
        */

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
                            halfwidth = resources.displayMetrics.widthPixels / 2

                            //The check for abs(Xdiff) < 10 && abs(YDiff) < 10 is because sometime elements moves a little while clicking.
                            //So that is click event.
                            // ON CLICK:
                            if (abs(Xdiff) < 10 && abs(Ydiff) < 10) {
                                //Set Recording mode:
                                overlayButton.setBackgroundResource(R.drawable.rounded_button_2)
                                countdownStart(overlayButton, sharedPrefs)

                            } else if ((abs(event.rawY) >= (height-200)) && (abs(event.rawX) >= (halfwidth-200)) && (abs(event.rawX) <= (halfwidth+200))) {
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
                            params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                            params!!.y = initialY + (event.rawY - initialTouchY).toInt()

                            //Grey out when swiped down:
                            if ((abs(event.rawY) >= (height-200)) && (abs(event.rawX) >= (halfwidth-200)) && (abs(event.rawX) <= (halfwidth+200))) {
                                overlayButton.setBackgroundResource(R.drawable.rounded_button_3)
                                overlayIcon.setImageResource(R.drawable.stop_icon)
                            } else {
                                //Back to green:
                                overlayButton.setBackgroundResource(R.drawable.rounded_button)
                                overlayIcon.setImageResource(R.drawable.record_icon)
                            }

                            //Update the layout with new X & Y coordinate
                            mWindowManager!!.updateViewLayout(mFloatingView, params)
                            return true
                        }
                    }
                    return false
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mFloatingView != null) mWindowManager!!.removeView(mFloatingView)
    }

    private fun startMyOwnForeground() {
        //Foreground service:
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.djeenoForSpotify"
        val channelName = "My Background Service"
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
            .setContentTitle("App is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    fun countdownStart(resource: RelativeLayout, sharedPrefs: SharedPreferences) {
        //get updated preferences:
        valRecTimeout = sharedPrefs.getString(SettingsActivity.KEY_REC_TIMEOUT, "5") as String
        spotifyToken = sharedPrefs.getString(SettingsActivity.KEY_SPOTIFY_TOKEN, "") as String

        //prepare thread:
        val mThread = Thread {
            try {
                synchronized(this) {
                    Thread.sleep(valRecTimeout!!.toLong() * 1000)   //default: 5000
                    //AFTER RECORDING:
                    //Reset overlay accent color:
                    resource.setBackgroundResource(R.drawable.rounded_button)
                    //Open links and redirects:
                    openResults(sharedPrefs)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
            }
        }
        //start thread:
        mThread.start()
    }

    private fun openResults(sharedPrefs: SharedPreferences){
        //get updated preferences:
        navEnabled = sharedPrefs.getBoolean(MainActivity.KEY_NAV_ENABLED, false)

        //Spotify result:
        val spotifyToOpen = "https://open.spotify.com/track/3jFP1e8IUpD9QbltEI1Hcg?si=pt790-QFRyWr2JhyoMb_yA"

        //Maps redirect:
        if (navEnabled) {
            switchToMaps(sharedPrefs)
        }
        //Open query result in Spotify:
        val intentSpotify = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(spotifyToOpen)
        )
        intentSpotify.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentSpotify.putExtra("fromwhere", "ser")
        startActivity(intentSpotify)
    }

    private fun switchToMaps(sharedPrefs: SharedPreferences){
        //get updated preferences:
        valMapsTimeout = sharedPrefs.getString(SettingsActivity.KEY_MAPS_TIMEOUT, "3") as String
        mapsAddress = sharedPrefs.getString(SettingsActivity.KEY_MAPS_ADDRESS, "https://www.google.com/maps/") as String

        //prepare thread:
        val mThread = Thread {
            try {
                synchronized(this) {
                    Thread.sleep(valMapsTimeout!!.toLong() * 1000)   //default: 3000
                    //Launch Maps:
                    val mapIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(mapsAddress)
                    )
                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    mapIntent.putExtra("fromwhere", "ser")
                    startActivity(mapIntent)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
            }
        }
        //start thread:
        mThread.start()
    }
}