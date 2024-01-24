package com.ftrono.DJames.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
            //Toast.makeText(context, "Screen Off.", Toast.LENGTH_SHORT).show()
        }

        //when screen is on
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            screenOn = true
            Log.d(TAG, "Screen On.")
            //Toast.makeText(context, "Screen On.", Toast.LENGTH_SHORT).show()
        }
    }

}