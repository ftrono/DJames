package com.ftrono.DJames.be.audio

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import com.ftrono.DJames.application.audioManager


class AudioRequestsManager() {
    private val TAG = this::class.java.simpleName
    var requestsStack: MutableList<AudioFocusRequest> = mutableListOf()

    //AudioManager:
    private val duckedAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val exclusiveAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()


    // Helpers:
    private fun getAudioFocusChangeListener(
        onFail: () -> Unit
    ): AudioManager.OnAudioFocusChangeListener {
        return AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> onFail()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onFail()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onFail()
                AudioManager.AUDIOFOCUS_GAIN -> Log.d(TAG, "Audio focus re-gained.")
            }
        }
    }


    private fun requestFocus(
        focusRequest: AudioFocusRequest,
        onGranted: () -> Unit,
        onFail: () -> Unit
    ) {
        try {
            //REQUEST AUDIO FOCUS:
            val res = audioManager!!.requestAudioFocus(focusRequest)
            when (res) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    Log.d(TAG, "Cannot gain audio focus! Try again.")
                    onFail()
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    Log.d(TAG, "Audio focus granted!")
                    requestsStack.add(focusRequest)
                    onGranted()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AUDIOFOCUS: EXCEPTION: ", e)
        }
    }


    // MAIN:
    // REQUEST DUCKED FOCUS:
    fun requestDuckedFocus(
        onGranted: () -> Unit,
        onFail: () -> Unit
    ) {
        try {
            // Build DUCKED focus request:
            val duckedFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(duckedAttributes)
                .setOnAudioFocusChangeListener(
                    getAudioFocusChangeListener { onFail() }
                )
                .build()

            //REQUEST AUDIO FOCUS:
            requestFocus(
                duckedFocusRequest,
                onGranted = { onGranted() },
                onFail = {
                    // Fallback to exclusive focus:
                    requestExclusiveFocus(
                        onGranted = { onGranted() },
                        onFail = { onFail() },
                    )
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "AUDIOFOCUS: EXCEPTION: ", e)
        }
    }


    // REQUEST EXCLUSIVE FOCUS:
    fun requestExclusiveFocus(
        onGranted: () -> Unit,
        onFail: () -> Unit
    ) {
        try {
            // Build EXCLUSIVE focus request:
            val exclusiveFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(exclusiveAttributes)
                .setOnAudioFocusChangeListener(
                    getAudioFocusChangeListener { onFail() }
                )
                .build()

            //REQUEST AUDIO FOCUS:
            requestFocus(
                exclusiveFocusRequest,
                { onGranted() },
                { onFail() }
            )
        } catch (e: Exception) {
            Log.w(TAG, "AUDIOFOCUS: EXCEPTION: ", e)
        }
    }


    //RELEASE AUDIO FOCUS:
    fun releaseAudioFocus() {
        // Clean stack:
        for (req in requestsStack.reversed()) {
            try {
                audioManager!!.abandonAudioFocusRequest(req)
            } catch (e: Exception) {
                Log.w(TAG, "ERROR: AudioFocus - Focus already released for the current request.")
            }
            requestsStack.removeAt(requestsStack.lastIndex)
        }
    }

}