package com.ftrono.DJames.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.*


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
                //If not voiceQueryOn and not callMode:
                if (!callMode && vol_initialized && screenOn) {
                    if (!voiceQueryOn) {
                        //START VOICE QUERY SERVICE:
                        sourceIsVolume = true
                        try {
                            context!!.startService(Intent(context, VoiceQueryService::class.java))
                            Log.d(TAG, "EVENT: VOLUME_CHANGED_ACTION: VOICE QUERY SERVICE STARTED.")
                        } catch (e:Exception) {
                            Log.w(TAG, "ERROR: VOLUME_CHANGED_ACTION: VOICE QUERY SERVICE NOT STARTED. ", e)
                        }
                    } else if (recordingMode) {
                        //EARLY STOP RECORDING:
                        Intent().also { intent ->
                            intent.setAction(ACTION_REC_STOP)
                            context!!.sendBroadcast(intent)
                        }
                    }
                }
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

        //TOASTER:
        if (intent.action == ACTION_TOASTER) {
            Log.d(TAG, "EVENT: ACTION_TOASTER.")
            var toastText = intent.getStringExtra("toastText")
            //TOAST:
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }

    }

}