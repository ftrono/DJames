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
    private var clockView: TextView? = null
    private var now: LocalDateTime? = null
    private val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")
    private var clockSeparator: String = "\n"
    private var prevBrightness: Int = 255

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fake_lock_screen)

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Vertical:
            clockSeparator = "\n"
        }
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Horizontal:
            clockSeparator = ":"
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

        //Exit button:
        val exitButton = findViewById<Button>(R.id.exit_button)
        exitButton.setOnClickListener(View.OnClickListener {
            finish()
        })

        //Date:
        val dateView = findViewById<TextView>(R.id.text_date)
        clockView = findViewById<TextView>(R.id.text_clock)

        //Update date & clock:
        val handler = Handler()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                now = LocalDateTime.now()
                dateView.text = now!!.format(dateFormat)
                clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Vertical:
            clockSeparator = "\n"
            clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Horizontal:
            clockSeparator = ":"
            clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Settings.System.putInt(
            applicationContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            prevBrightness
        )
    }

}