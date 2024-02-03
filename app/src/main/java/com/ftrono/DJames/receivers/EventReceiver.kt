package com.ftrono.DJames.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.ftrono.DJames.service.VoiceSearchService


class EventReceiver: BroadcastReceiver() {

    private val TAG = EventReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {

        //volumeUp receiver:
        if (intent!!.action == "android.media.VOLUME_CHANGED_ACTION") {
            val newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)
            val oldVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", 0)
            val event = intent.getIntExtra("android.intent.extra.KEY_EVENT", 0)

            if (newVolume >= oldVolume && !recordingMode && screenOn) {

                //START VOICE SEARCH SERVICE:
                recordingMode = true
                context!!.startService(Intent(context, VoiceSearchService::class.java))

                Log.d(
                    TAG,
                    "VOLUME_UP BUTTON PRESSED, KEY EVENT: ${event}."
                )
            }
        }

        //when screen is off
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            screenOn = false
            Log.d(TAG, "Screen Off.")
        }

        //when screen is on
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            screenOn = true
            Log.d(TAG, "Screen On.")
        }

        //when logged in:
        if (intent.action == ACTION_LOGGED_IN) {
            Log.d(TAG, "ACTION_LOGGED_IN.")
            try {
                loggedIn = true
                //Set Logged-In UI:
                if (loginButton != null) {
                    loginButton!!.setTitle("Logout")
                }
                mainActionBar!!.subtitle = "for ${prefs.userName}"
                descr_login_status!!.text = context!!.getString(R.string.str_status_logged)
                face_cover!!.visibility = View.INVISIBLE
                startButton!!.text = "S T A R T"
                startButton!!.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorAccent)
                descr_main!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.light_grey))
                descr_main!!.setTypeface(null, Typeface.ITALIC)
                descr_main!!.text = context.resources.getString(R.string.str_main_start)
                descr_use!!.text = context.resources.getString(R.string.str_use_logged)
                descr_use!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.mid_grey))

                Toast.makeText(context, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_LOGGED_IN: resources not available.")
            }
        }

        //when overlay status changed:
        if (intent.action == ACTION_OVERLAY_DEACTIVATED) {
            Log.d(TAG, "ACTION_OVERLAY_DEACTIVATED.")
            try {
                overlay_active = false
                startButton!!.text = "S T A R T"
                startButton!!.backgroundTintList = AppCompatResources.getColorStateList(context!!, R.color.colorAccent)
                descr_main!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.light_grey))
                descr_main!!.setTypeface(null, Typeface.ITALIC)
                descr_main!!.text = context.resources.getString(R.string.str_main_start)
                descr_use!!.text = context.resources.getString(R.string.str_use_logged)
                descr_use!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.mid_grey))
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_OVERLAY_DEACTIVATED: resources not available.")
            }
        }

        //when Fake Lock Screen is opened:
        if (intent.action == ACTION_CLOCK_OPENED) {
            Log.d(TAG, "ACTION_CLOCK_OPENED.")
            if (!recordingMode) {
                try {
                    overlayButton!!.setBackgroundResource(R.drawable.rounded_button_dark)
                    overlayIcon!!.setImageResource(R.drawable.speak_icon_gray)
                    overlayClockButton!!.visibility = View.INVISIBLE
                    overlayClockText!!.visibility = View.INVISIBLE
                } catch (e: Exception) {
                    Log.d(TAG, "ACTION_CLOCK_OPENED: resources not available.")
                }
            }
        }

        //when Fake Lock Screen is closed:
        if (intent.action == ACTION_CLOCK_CLOSED) {
            Log.d(TAG, "ACTION_CLOCK_CLOSED.")
            if (!recordingMode) {
                try {
                    overlayButton!!.setBackgroundResource(R.drawable.rounded_button_ready)
                    overlayIcon!!.setImageResource(R.drawable.speak_icon)
                    overlayClockButton!!.visibility = View.VISIBLE
                    overlayClockText!!.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.d(TAG, "ACTION_CLOCK_CLOSED: resources not available.")
                }
            }
        }

        //when a new song is played:
        if (intent.action == ACTION_NEW_SONG) {
            Log.d(TAG, "ACTION_NEW_SONG.")
            if (clock_active) {
                try {
                    //Populate player info:
                    songView!!.text = songName
                    artistView!!.text = artistName
                    contextView!!.text = contextName
                } catch (e: Exception) {
                    Log.d(TAG, "ACTION_NEW_SONG: resources not available.")
                }
            }
        }

    }

}