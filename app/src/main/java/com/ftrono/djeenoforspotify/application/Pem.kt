package com.ftrono.djeenoforspotify.application

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.djeenoforspotify.R

class Pem : AppCompatActivity() {

    val TAG = Pem::class.java.simpleName

    val mThread = Thread {
        try {
            synchronized(this) {
                Thread.sleep(1500)
                backToMain()
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pem)

        cuteddhra()

        mThread.start()

    }

    fun cuteddhra(){

        // Notification service:
        val notify =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification =
            Notification.Builder(applicationContext)
                .setContentTitle("CUTEDDHRATA!!!")
                .setContentText("PPPEEEEEMMM!!!")
                .setSmallIcon(R.drawable.app_icon_round)
                .build()

        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        notify.notify(0, notification)


        // Vibration service:
        val DELAY = 0
        val VIBRATE = 1000
        val SLEEP = 1000
        val vibratePattern = longArrayOf(DELAY.toLong(), VIBRATE.toLong(), SLEEP.toLong())
        val vibrationEffect1 = VibrationEffect.createWaveform(vibratePattern, -1)
        //val vibrationEffect1 = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= 31) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.getDefaultVibrator()
        }

        vibrator.cancel()
        vibrator.vibrate(vibrationEffect1)
    }

    fun backToMain(){

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

    }

}
