package com.ftrono.djeenoforspotify.application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.djeenoforspotify.R


class Splash : AppCompatActivity() {

    val TAG = Splash::class.java.simpleName

    val mThread = Thread {
        try{
            synchronized(this){
            Thread.sleep(2000)
            goToNext()}
        } catch (e: InterruptedException){
            Log.d(TAG, "Interrupted: exception.", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.splash)

        val cutSplash = findViewById<View>(R.id.cut_splash)
        val splashTitle = findViewById<View>(R.id.splash_title)
        val splashSubtitle = findViewById<View>(R.id.splash_subtitle)

        cutSplash.setOnClickListener {
            goToNext()
        }

        mThread.start()

    }


    fun goToNext() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mThread.interrupt()
        Log.d(TAG, "onDestroy()")
    }


}



