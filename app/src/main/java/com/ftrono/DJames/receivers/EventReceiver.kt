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
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation


class EventReceiver: BroadcastReceiver() {

    private val TAG = EventReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {

        //volumeUp receiver:
        if (intent!!.action == "android.media.VOLUME_CHANGED_ACTION") {
            val newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)
            val oldVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", 0)
            val event = intent.getIntExtra("android.intent.extra.KEY_EVENT", 0)

            if (newVolume >= oldVolume && !recordingMode && screenOn && prefs.volumeUpEnabled) {
                //START VOICE SEARCH SERVICE:
                recordingMode = true
                sourceIsVolume = true
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
            loggedIn = true

            //MAIN ACTIVITY:
            try {
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
                if (prefs.volumeUpEnabled) {
                    descr_use!!.text = context.resources.getString(R.string.str_use_logged)
                } else {
                    descr_use!!.text = context.resources.getString(R.string.str_use_logged_no_vol)
                }
                descr_use!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.mid_grey))
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_LOGGED_IN: Main() resources not available.")
            }

            //SETTINGS ACTIVITY:
            try {
                login_mini_button!!.text = "LOGOUT"
                userNameView!!.text = prefs.userName
                userEMailView!!.visibility = View.VISIBLE
                userEMailView!!.text = prefs.userEMail
                if (prefs.userImage != "") {
                    Picasso.get().load(prefs.userImage)
                        .transform(CropCircleTransformation())
                        .into(userIcon)
                }
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_LOGGED_IN: Settings() resources not available.")
            }

            //End:
            Toast.makeText(context, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
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

        //when Settings VOLUME-UP is changed:
        if (intent.action == ACTION_VOLUME_UP_CHANGED) {
            Log.d(TAG, "ACTION_VOLUME_UP_CHANGED.")
            try {
                if (prefs.volumeUpEnabled) {
                    descr_use!!.text = context!!.resources.getString(R.string.str_use_logged)
                } else {
                    descr_use!!.text = context!!.resources.getString(R.string.str_use_logged_no_vol)
                }
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_VOLUME_UP_CHANGED: resources not available.")
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
                    overlayClockIcon!!.visibility = View.INVISIBLE
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
                    overlayClockIcon!!.visibility = View.VISIBLE
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
                    if (artwork != "") {
                        Picasso.get().load(artwork)
                            .into(artworkView)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ACTION_NEW_SONG: resources not available.")
                }
            }
        }

    }

}