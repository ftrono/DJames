package com.ftrono.DJames.be.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.models.AiReply
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TTSReader(private val context: Context) {
    //Main:
    private val TAG = TTSReader::class.java.simpleName

    fun speak(
        aiReplies: List<AiReply>
    ) {
        var fullText = ""
        // Speak:
        runBlocking {
            for (reply in aiReplies) {
                fullText = fullText + reply.text
                ttsRead(context, reply)
            }
        }
        // Save reply:
        fulfillmentUtils.saveLogMessage(
            type = "ai",
            text = fullText
        )
    }

    //HELPER: TTS directly read:
    private suspend fun ttsRead(
        context: Context,
        aiReply: AiReply
    ): Unit = suspendCoroutine { continuation ->

        //Set Locale:
        var locale = Locale.UK
        if (aiReply.langCode == "it") {
            locale = Locale.ITALY
        }
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->

            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(TAG, "Language ${aiReply.langCode} not supported!")

                } else {
                    tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) {
                        }

                        override fun onDone(utteranceId: String) {
                            continuation.resume(Unit)
                        }

                        override fun onError(utteranceId: String) {
                            Log.e(TAG, "Error on $utteranceId")
                            //continuation.resume(Unit)
                        }
                    })
                    tts!!.speak(aiReply.text, TextToSpeech.QUEUE_FLUSH, null, utils.generateRandomString(8))
                }
            } else {
                Log.e(TAG, "Initialization Failed!")
                continuation.resume(Unit)
            }
        }
    }

}