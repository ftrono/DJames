package com.ftrono.DJames.be.nlp

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.audioAttributes
import com.ftrono.DJames.application.audioFocusChangeListener
import com.ftrono.DJames.application.audioFocusRequest
import com.ftrono.DJames.application.audioManager
import com.ftrono.DJames.application.gMapsLinkFormat
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.Route
import com.ftrono.DJames.be.models.DispatcherInfo
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FulfillmentUtils {
    private val TAG = FulfillmentUtils::class.java.simpleName


    //FALLBACK:
    fun fallback(toastText: String = ""): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()
        dispatcherInfo.fail = true
        if (toastText != "") {
            dispatcherInfo.toastText = toastText
        }
        //Log.d(TAG, "dispatcherInfo: $dispatcherInfo")
        return dispatcherInfo
    }


    //Release AudioFocus
    fun releaseAudioFocus() {
        try {
            audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: AudioFocus already released.")
        }
    }


    //HELPER: TTS directly read:
    private suspend fun ttsReadNoFocus(context: Context, item: Map<String, String>): Unit = suspendCoroutine { continuation ->
        //SET UP TTS:
        val langCode = item["language"]
        val text = item["text"]
        //Set Locale:
        var locale = Locale.UK
        if (langCode == "it") {
            locale = Locale.ITALY
        }
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(TAG, "Language ${langCode} not supported!")
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
                    tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, utils.generateRandomString(8))
                }
            } else {
                Log.e(TAG, "Initialization Failed!")
                continuation.resume(Unit)
            }
        }
    }


    //HELPER: TTS read with audio dimming:
    private fun ttsReadWithFocus(
        context: Context,
        audioFocusRequest: AudioFocusRequest,
        items: List<Map<String, String>>
    ) {
        //BUILD FOCUS REQUEST:
        val focusRequest = audioManager!!.requestAudioFocus(audioFocusRequest)
        when (focusRequest) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Log.d(TAG, "Cannot gain audio focus! Try again.")
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                runBlocking {
                    for (item in items) {
                        ttsReadNoFocus(context, item)
                    }
                }
                audioManager!!.abandonAudioFocusRequest(audioFocusRequest)
            }

            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                //mAudioFocusPlaybackDelayed = true
            }
        }
    }

    //TTS READER: MAIN FUNCTION:
    fun ttsRead(context: Context, items: List<Map<String, String>>, dimAudio: Boolean = false) {
        try {
            Thread.sleep(1000)
            if (dimAudio) {
                //Build AudioFocus dim request:
                audioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(audioAttributes!!)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build()
                ttsReadWithFocus(context, audioFocusRequest!!, items)
            } else {
                runBlocking {
                    for (item in items) {
                        ttsReadNoFocus(context, item)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ttsRead: cannot read TTS. EXCEPTION: ", e)
        }
    }


    //Replace nums with strings (& opposite):
    fun replaceNums(text: String): String {
        //TODO: Replace words with numbers too.
        val regexMap = mapOf(
            "\\d*000\\b" to "thousand",
            "\\d*,000\\b" to "thousand",
            "\\d*00\\b" to "hundred",
            "thousand" to "1000",
            "hundred" to "100"
        )

        var number = false
        var newText = text
        val textSplits = text.lowercase().split(" ")
        for (tok in textSplits) {
            if (tok == "number") {
                number = true
            }
            //tok -> entire word:
            for (regStr in regexMap.keys) {
                val regex = Regex(regStr)
                //match -> only fixed portion:
                val match = regex.find(tok)?.value
                if (match != null) {
                    Log.d(TAG, "REGEX MATCHED: $match")
                    //If match: if "number", remove the word "number" and
                    if (number) {
                        newText = newText.replace("number", "")
                    } else {
                        //val cleaned = tok.replace(match, "").replace(",", "")
                        newText = newText.replace(tok, regexMap[regStr]!!)
                    }
                    break
                }
            }
        }
        return newText
    }


    //Emoji replacer:
    fun replaceEmojis(context: Context, text: String, reqLanguage: String): String {
        var textReplaced = text
        var reader: BufferedReader? = null
        //Query language:
        var messLanguage = prefs.messageLanguage
        if (reqLanguage != "") {
            messLanguage = reqLanguage
        }
        //language:
        if (messLanguage == "it") {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_ita)))   //"ita"
        } else {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_eng)))   //"eng"
        }

        //Load map:
        val sourceSents = JsonParser.parseReader(reader).asJsonObject
        val emojiMap = sourceSents.get("emojiMap").asJsonObject

        //Replace:
        textReplaced = textReplaced.replace("Emoji", "emoji")
        textReplaced = textReplaced.replace("emoji,", "emoji")
        textReplaced = textReplaced.replace("emoji.", "emoji")
        textReplaced = textReplaced.replace("emoji?", "emoji")

        for (sent in emojiMap.keySet()) {
            textReplaced = textReplaced.replace("emoji $sent", emojiMap.get(sent).asString, ignoreCase=true)
        }
        return textReplaced
    }


    //Route: build navigation URL from Library Route item:
    fun buildRouteUrlFromLibraryItem(item: Route): String {
        var url = gMapsLinkFormat
        //Via:
        var fullVia = listOf(
                item.via.placeName,
                item.via.address,
                item.via.number,
                item.via.zip,
                item.via.town,
                item.via.province
            ).joinToString(" ").trim()
        if (fullVia != "") {
            fullVia = fullVia + "/"
        }

        //Destination:
        var fullDestination = listOf(
            item.destination.placeName,
            item.destination.address,
            item.destination.number,
            item.destination.zip,
            item.destination.town,
            item.destination.province
        ).joinToString(" ").trim()

        return url + fullVia.replace(" ", "+") + fullDestination.replace(" ", "+")

    }


    //Route: build navigation URL from temp ItemInfo item:
    fun buildRouteUrlFromItemInfo(item: ItemInfoUse): String {
        var url = gMapsLinkFormat
        var viaUrl = ""
        if (item.detail != "") {
            viaUrl = item.detail.replace(" ", "+") + "/"
        }
        url = url + viaUrl + item.name.replace(" ", "+").trim() + "/"
        return url
    }

}