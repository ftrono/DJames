package com.ftrono.DJames.application

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.res.Configuration
import android.provider.Settings
import com.ftrono.DJames.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.view.WindowCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import kotlin.math.round
import kotlin.math.roundToInt


class FakeLockScreen: AppCompatActivity() {

    private val TAG: String = FakeLockScreen::class.java.getSimpleName()

    private var handler: Handler? = null
    private var runnable: Runnable? = null

    private var clockView: TextView? = null
    private var exitButtonVert: View? = null
    private var songName: TextView? = null
    private var artistName: TextView? = null
    private var contextName: TextView? = null

    private var density: Float = 0F
    private var now: LocalDateTime? = null
    private val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")
    private var clockSeparator: String = "\n"
    private var prevBrightness: Int = 255


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fake_lock_screen)

        //Load views:
        clockView = findViewById<TextView>(R.id.text_clock)
        exitButtonVert = findViewById<View>(R.id.exit_button)
        songName = findViewById<TextView>(R.id.song_name)
        artistName = findViewById<TextView>(R.id.artist_name)
        contextName = findViewById<TextView>(R.id.context_name)

        //Screen density:
        density = applicationContext.resources.displayMetrics.density

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Vertical:
            clockSeparator = "\n"
        }
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Horizontal:
            clockSeparator = ":"
            //Move exit button to the LEFT:
            exitButtonVert!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                setMargins((50*density).roundToInt(),0,0,0)
            }
            //Fix constraint for last textView:
            contextName!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomToTop = ConstraintLayout.LayoutParams.UNSET   //clear
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            //Fix Clock text size:
            clockView!!.textSize = 140F
        }

        //Store prev brightness level:
        prevBrightness = Settings.System.getInt(
            applicationContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        Log.d(TAG, "Brightness: $prevBrightness")

        Settings.System.putInt(
            applicationContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            round(((prevBrightness/3)*2).toDouble()).toInt()
        )

        val test = Settings.System.getInt(
            applicationContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        Log.d(TAG, "Brightness: $test")

        //Hide status bar:
        val mainContainer = findViewById<ConstraintLayout>(R.id.fake_lock_container)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, mainContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        //Exit buttons listeners:
        exitButtonVert!!.setOnClickListener(View.OnClickListener {
            finish()
        })

        //Date:
        val dateView = findViewById<TextView>(R.id.text_date)

        //Update date & clock:
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                now = LocalDateTime.now()
                dateView.text = now!!.format(dateFormat)
                clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
                handler!!.postDelayed(this, 5000)
            }
        }
        handler!!.post(runnable!!)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setVerticalView()
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setHorizontalView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler!!.removeCallbacks(runnable!!)
        Settings.System.putInt(
            applicationContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            prevBrightness
        )
        Log.d(TAG, "Brightness reset to: $prevBrightness")
    }

    fun setVerticalView() {
        //Vertical:
        clockSeparator = "\n"
        clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        //Move exit button to the BOTTOM:
        exitButtonVert!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToTop = ConstraintLayout.LayoutParams.UNSET   //clear
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.context_name
            setMargins(0,(40*density).roundToInt(),0,0)
        }
        //Fix constraint for last textView:
        contextName!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
            bottomToTop = R.id.exit_button
        }
        //Fix Clock text size:
        clockView!!.textSize = 150F
    }

    fun setHorizontalView() {
        //Horizontal:
        clockSeparator = ":"
        clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        //Move exit button to the LEFT:
        exitButtonVert!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
            topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            setMargins((50*density).roundToInt(),0,0,0)
        }
        //Fix constraint for last textView:
        contextName!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToTop = ConstraintLayout.LayoutParams.UNSET   //clear
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        //Fix Clock text size:
        clockView!!.textSize = 140F
    }

}