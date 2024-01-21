package com.ftrono.djeenoforspotify.application

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.djeenoforspotify.R
import java.time.format.DateTimeFormatter
import android.os.Handler
import androidx.constraintlayout.widget.ConstraintLayout
import java.time.LocalDateTime
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat


class FakeLockScreen: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fake_lock_screen)

        //Immersive full screen:
        val mainContainer = findViewById<ConstraintLayout>(R.id.fake_lock_container)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, mainContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Toast.makeText(applicationContext, "Use the overlay button to record voice requests.", Toast.LENGTH_SHORT).show()

        //Exit button:
        val exitButton = findViewById<Button>(R.id.exit_button)
        exitButton.setOnClickListener(View.OnClickListener {
            finish()
        })

        //Clock:
        var now: LocalDateTime? = null

        //Date:
        val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
        val dateView = findViewById<TextView>(R.id.text_date)

        //Hour:
        val hourFormat = DateTimeFormatter.ofPattern("HH")
        val hourView = findViewById<TextView>(R.id.text_hour)

        //Min:
        val minsFormat = DateTimeFormatter.ofPattern("mm")
        val minsView = findViewById<TextView>(R.id.text_mins)

        //Update date & clock:
        val handler = Handler()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                now = LocalDateTime.now()
                dateView.text = now!!.format(dateFormat)
                hourView.text = now!!.format(hourFormat)
                minsView.text = now!!.format(minsFormat)
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable)
    }

}