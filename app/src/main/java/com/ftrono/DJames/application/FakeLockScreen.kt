package com.ftrono.DJames.application

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
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
import kotlin.math.round


class FakeLockScreen: AppCompatActivity() {

    private val TAG: String = FakeLockScreen::class.java.getSimpleName()

    private var handler: Handler? = null
    private var runnable: Runnable? = null

    private var clockView: TextView? = null
    private var now: LocalDateTime? = null
    private val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")
    private var clockSeparator: String = "\n"
    private var prevBrightness: Int = 255

    private var exitButtonVert: View? = null
    private var exitIconVert: View? = null
    private var exitButtonHoriz: View? = null
    private var exitIconHoriz: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fake_lock_screen)

        //Exit buttons:
        exitIconVert = findViewById<View>(R.id.exit_icon_vert)
        exitButtonVert = findViewById<View>(R.id.exit_button_vert)
        exitIconHoriz = findViewById<View>(R.id.exit_icon_horiz)
        exitButtonHoriz = findViewById<View>(R.id.exit_button_horiz)

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Vertical:
            clockSeparator = "\n"
            exitButtonVert!!.visibility = View.VISIBLE
            exitIconVert!!.visibility = View.VISIBLE
            exitButtonHoriz!!.visibility = View.GONE
            exitIconHoriz!!.visibility = View.GONE
        }
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Horizontal:
            clockSeparator = ":"
            exitButtonVert!!.visibility = View.GONE
            exitIconVert!!.visibility = View.GONE
            exitButtonHoriz!!.visibility = View.VISIBLE
            exitIconHoriz!!.visibility = View.VISIBLE
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
        exitButtonHoriz!!.setOnClickListener(View.OnClickListener {
            finish()
        })

        //Date:
        val dateView = findViewById<TextView>(R.id.text_date)
        clockView = findViewById<TextView>(R.id.text_clock)

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
        //Move exit button:
        exitButtonVert!!.visibility = View.VISIBLE
        exitIconVert!!.visibility = View.VISIBLE
        exitButtonHoriz!!.visibility = View.GONE
        exitIconHoriz!!.visibility = View.GONE
    }

    fun setHorizontalView() {
        //Horizontal:
        clockSeparator = ":"
        clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        //Move exit button:
        exitButtonVert!!.visibility = View.GONE
        exitIconVert!!.visibility = View.GONE
        exitButtonHoriz!!.visibility = View.VISIBLE
        exitIconHoriz!!.visibility = View.VISIBLE
    }

}