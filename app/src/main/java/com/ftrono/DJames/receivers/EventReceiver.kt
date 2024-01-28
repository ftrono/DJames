package com.ftrono.DJames.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.TextView
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
                descr_use!!.text = context.getString(R.string.str_use_logged)
                //Show views:
                mapsView!!.visibility = View.VISIBLE
                mapsDescr!!.visibility = TextView.VISIBLE
                mapsTitle!!.visibility = TextView.VISIBLE
                clockView!!.visibility = View.VISIBLE
                clockDescr!!.visibility = TextView.VISIBLE
                clockTitle!!.visibility = TextView.VISIBLE
                Toast.makeText(context, "SUCCESS: Djames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_LOGGED_IN: resources not available.")
            }
        }

        //when mode is changed:
        if (intent.action == ACTION_MODE_CHANGED) {
            Log.d(TAG, "ACTION_MODE_CHANGED.")
            try {
                if (prefs.navEnabled) {
                    //MAPS ON:
                    mapsView!!.setBackgroundResource(R.drawable.rounded_option_sel)
                    mapsTitle!!.setTextColor(
                        AppCompatResources.getColorStateList(
                            context!!,
                            R.color.colorHeader
                        )
                    )
                    clockView!!.setBackgroundResource(R.drawable.rounded_option)
                    clockTitle!!.setTextColor(
                        AppCompatResources.getColorStateList(
                            context,
                            R.color.mid_grey
                        )
                    )
                } else {
                    //CLOCK ON:
                    mapsView!!.setBackgroundResource(R.drawable.rounded_option)
                    mapsTitle!!.setTextColor(
                        AppCompatResources.getColorStateList(
                            context!!,
                            R.color.mid_grey
                        )
                    )
                    clockView!!.setBackgroundResource(R.drawable.rounded_option_sel)
                    clockTitle!!.setTextColor(
                        AppCompatResources.getColorStateList(
                            context,
                            R.color.colorHeader
                        )
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_MODE_CHANGED: resources not available.")
            }
        }

        //when overlay status changed:
        if (intent.action == ACTION_OVERLAY_DEACTIVATED) {
            Log.d(TAG, "ACTION_OVERLAY_DEACTIVATED.")
            try {
                //ALL MODES OFF:
                mapsView!!.setBackgroundResource(R.drawable.rounded_option)
                mapsTitle!!.setTextColor(
                    AppCompatResources.getColorStateList(
                        context!!,
                        R.color.mid_grey
                    )
                )
                clockView!!.setBackgroundResource(R.drawable.rounded_option)
                clockTitle!!.setTextColor(
                    AppCompatResources.getColorStateList(
                        context,
                        R.color.mid_grey
                    )
                )
            } catch (e: Exception) {
                Log.d(TAG, "ACTION_OVERLAY_DEACTIVATED: resources not available.")
            }
        }

    }

}