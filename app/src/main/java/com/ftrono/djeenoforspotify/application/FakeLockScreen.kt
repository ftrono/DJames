package com.ftrono.djeenoforspotify.application

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.res.Configuration
import com.ftrono.djeenoforspotify.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class FakeLockScreen: AppCompatActivity() {

    private var isHorizontal: Boolean = false
    private var clockView: TextView? = null
    private var now: LocalDateTime? = null
    private val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")
    private var clockSeparator: String = "\n"

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

        /*
        //Immersive full screen:
        val mainContainer = findViewById<ConstraintLayout>(R.id.fake_lock_container)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, mainContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        */

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

}