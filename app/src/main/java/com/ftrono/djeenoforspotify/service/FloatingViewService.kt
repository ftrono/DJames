package com.ftrono.djeenoforspotify.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
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
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ftrono.djeenoforspotify.R
import com.ftrono.djeenoforspotify.application.prefs
import com.ftrono.djeenoforspotify.service.VoiceSearchService
import java.io.File
import kotlin.math.abs


class FloatingViewService : Service() {
    private val TAG = FloatingViewService::class.java.simpleName

    //View managers:
    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null
    private var mCloseView: View? = null
    private var params: LayoutParams? = null
    private var params2: LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        try {
            startForeground()

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
            val overlayButton =
                mFloatingView!!.findViewById<View>(R.id.rounded_button) as RelativeLayout
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
                                    if (!isMyServiceRunning(VoiceSearchService::class.java)) {
                                        //Start Voice Search service:
                                        startService(Intent(applicationContext, VoiceSearchService::class.java))
                                        waitForRecordDone(overlayButton, overlayIcon)
                                    }
                                } else if ((abs(event.rawY) >= (height - 200)) && (abs(event.rawX) >= (halfwidth - 200)) && (abs(
                                        event.rawX
                                    ) <= (halfwidth + 200))
                                ) {
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

//                                //Grey out when swiped down:
//                                if ((abs(event.rawY) >= (height - 200)) && (abs(event.rawX) >= (halfwidth - 200)) && (abs(
//                                        event.rawX
//                                    ) <= (halfwidth + 200))
//                                ) {
//                                    overlayButton.setBackgroundResource(R.drawable.rounded_button_3)
//                                    overlayIcon.setImageResource(R.drawable.stop_icon)
//                                } else {
//                                    //Back to green:
//                                    overlayButton.setBackgroundResource(R.drawable.rounded_button)
//                                    overlayIcon.setImageResource(R.drawable.record_icon)
//                                }

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

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mFloatingView != null) mWindowManager!!.removeView(mFloatingView)
        } catch (e: Exception) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    private fun startForeground() {
        //Foreground service:
        val NOTIFICATION_CHANNEL_ID = "com.ftrono.djeenoForSpotify"
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
            .setContentTitle("Djeeno: Floating View Service is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
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

    fun waitForRecordDone(button: RelativeLayout, icon: ImageView) {
        //prepare thread:
        val mThread = Thread {
            try {
                synchronized(this) {
                    //Set Recording mode:
                    button.setBackgroundResource(R.drawable.rounded_button_2)
                    //RECORDING COUNTDOWN:
                    Thread.sleep(prefs.recTimeout.toLong() * 1000)   //default: 5000
                    //Reset overlay processing color:
                    button.setBackgroundResource(R.drawable.rounded_button_3)
                    icon.setImageResource(R.drawable.looking_icon)

                    //PROCESSING COUNTDOWN:
                    Thread.sleep(2000)
                    //Reset overlay accent color:
                    button.setBackgroundResource(R.drawable.rounded_button)
                    icon.setImageResource(R.drawable.record_icon)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
            }
        }
        //start thread:
        mThread.start()
    }

}