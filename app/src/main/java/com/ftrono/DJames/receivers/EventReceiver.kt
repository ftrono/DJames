package com.ftrono.DJames.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.*
import com.ftrono.DJames.service.VoiceSearchService


class EventReceiver: BroadcastReceiver() {

    private val TAG = EventReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {

        //VolumeUp receiver:
        if (intent!!.action == VOLUME_CHANGED_ACTION) {
            val newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)
            val oldVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", 0)

            if (newVolume >= oldVolume && prefs.volumeUpEnabled) {
                //If volume maximum -> Lower volume (to enable Receiver):
                if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) == streamMaxVolume) {
                    audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume-1, AudioManager.FLAG_PLAY_SOUND)
                    Log.d(TAG, "EVENT: VOLUME_CHANGED_ACTION: Volume lowered.")
                }
                //If not recordingMode and not callMode:
                if (!recordingMode && !callMode && vol_initialized && screenOn) {
                    //START VOICE SEARCH SERVICE:
                    recordingMode = true
                    sourceIsVolume = true
                    context!!.startService(Intent(context, VoiceSearchService::class.java))
                    Log.d(TAG,"EVENT: VOLUME_CHANGED_ACTION: VOICE SEARCH SERVICE STARTED.")
                }
            }
        }

        //Listen to phone state:
        if (intent.action == PHONE_STATE_ACTION) {
            Log.d(TAG, "EVENT: PHONE STATE CHANGED.")
            try {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    callMode = true
                    Log.d(TAG, "EVENT: CALL MODE ON.")
                }
                else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                    callMode = false
                    Log.d(TAG, "EVENT: CALL MODE OFF.")
                }
            } catch (e: Exception) {
                Log.d(TAG, "EVENT: PHONE STATE CHANGED: receiver error.")
            }
        }

        //When screen is off:
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            screenOn = false
            Log.d(TAG, "EVENT: Screen Off.")
        }

        //When screen is on:
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            screenOn = true
            Log.d(TAG, "EVENT: Screen On.")
        }

        //When NLP result received -> TOAST:
        if (intent.action == ACTION_NLP_RESULT) {
            Log.d(TAG, "EVENT: ACTION_NLP_RESULT.")
            //TOAST:
            if (nlp_queryText != "") {
                Toast.makeText(context, nlp_queryText.replaceFirstChar { it.uppercase() }, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Sorry, I did not understand!", Toast.LENGTH_LONG).show()
            }
        }

        //When redirect countdown activated -> TOAST:
        if (intent.action == ACTION_REDIRECT) {
            Log.d(TAG, "EVENT: ACTION_REDIRECT.")
            //TOAST:
            Toast.makeText(context, "Going back to Clock in ${prefs.clockTimeout} seconds...", Toast.LENGTH_LONG).show()
        }

    }

}