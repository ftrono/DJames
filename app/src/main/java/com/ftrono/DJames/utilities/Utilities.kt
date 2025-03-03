package com.ftrono.DJames.utilities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.util.Patterns
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.streams.asSequence


class Utilities {
    private val TAG = Utilities::class.java.simpleName

    //OkHTTP: make HTTP request:
    suspend fun makeRequest(client: OkHttpClient, request: Request): String = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response.body!!.string())
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume("")
                Log.d(TAG, "RESPONSE ERROR: ", e)
            }
        })
    }


    //Check service running:
    fun isMyServiceRunning(serviceClass: Class<*>, mContext: Context): Boolean {
        val manager = mContext.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    //ID creator:
    fun generateRandomString(length: Int, numOnly: Boolean = false): String {
        var source = ""
        if (numOnly) {
            source = "0123456789"
        } else {
            source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        }

        return Random().ints(length.toLong(), 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }


    //Capitalize each word in a sentence:
    fun capitalizeWords(text: String, delimiter: String = " "): String {
        return text.split(delimiter).joinToString(delimiter) { it.lowercase().replaceFirstChar(Char::titlecaseChar) }
    }


    //Check if string is made of alphabetic characters:
    fun isLetters(string: String): Boolean {
        return string.all { it.isLetter() }
    }


    //Validate artist URL:
    fun isArtistUrl(playUrl: String): Boolean {
        val urlTest = URLUtil.isValidUrl(playUrl) && Patterns.WEB_URL.matcher(playUrl).matches()
        //True if conditions are met:
        return (urlTest && playUrl.contains(artistUrlIntro))
    }


    //Validate playlist URL:
    fun isPlaylistUrl(playUrl: String): Boolean {
        val urlTest = URLUtil.isValidUrl(playUrl) && Patterns.WEB_URL.matcher(playUrl).matches()
        //True if conditions are met:
        return (urlTest && playUrl.contains(playlistUrlIntro))
    }


    //Validate contact phone (i.e. no phone number, no international prefix in phone number):
    fun isGlobalPhone(prefix: String, phone: String): Boolean {
        val phoneTest = PhoneNumberUtils.isGlobalPhoneNumber(phone)
        //True if conditions are met:
        //(Phone numbers length is 10 digits (Italy) or 11 digits (UK), + international prefix (3 digits)):
        return (phoneTest && (prefix.contains("+") && prefix.length == 3) && (phone.length == 10 || phone.length == 11))
    }


    //Trim strings:
    fun trimString(textOrig: String, maxLength: Int = 30): String {
        var textTrimmed = textOrig
        if (textOrig.length > (maxLength+3)) {
            textTrimmed = textOrig.slice(0..(maxLength)) + "..."
        }
        return textTrimmed
    }

    //Get stdev:
    fun getStDev(numbers: List<Int>): Int {
        val mean = numbers.average()
        val variance = numbers.map { (it - mean).pow(2) }.average()
        return sqrt(variance).roundToInt()
    }


    fun updateStatesMap(statesMap: SnapshotStateMap<String, Boolean>, target: String): SnapshotStateMap<String, Boolean> {
        for (k in statesMap.keys) {
            if (k == target) {
                statesMap[k] = true
            } else {
                statesMap[k] = false
            }
        }
        return statesMap
    }


    //From a detected language name, get the supported language code:
    fun getLanguageCode(context: Context, language: String, default: String): String {
        var reqLanguage = default
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.language_codes)))
        val sourceJson = JsonParser.parseReader(reader).asJsonObject
        if (sourceJson.has(language)) {
            reqLanguage = sourceJson[language].asString
        }
        return reqLanguage
    }


    //Get the corresponding language name in preferred language for the detected language code:
    fun getLanguageName(context: Context, languageCode: String): String {
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.language_names)))
        val prefLanguage = prefs.queryLanguage
        val sourceJson = JsonParser.parseReader(reader).asJsonObject.get(prefLanguage).asJsonObject
        return sourceJson[languageCode].asString
    }


    //Open new log:
    fun openLog() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        last_log = JsonObject()
        last_log!!.addProperty("datetime", now)
        last_log!!.addProperty("app_version", appVersion)
    }


    //Close last open log:
    fun closeLog(context: Context) {
        try {
            var now = last_log!!.get("datetime").asString
            var logFile = File(logDir, "$now.json")
            logFile.writeText(last_log.toString())
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_LOG_REFRESH)
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Log not saved!", e)
        }
    }

    //FALLBACK:
    fun fallback(toastText: String = ""): JsonObject {
        var processStatus = JsonObject()
        processStatus.addProperty("fail", true)
        if (toastText != "") {
            processStatus.addProperty("toastText", toastText)
        }
        //Log.d(TAG, "processStatus: $processStatus")
        return processStatus
    }


    //HELPER: TTS directly read:
    private suspend fun ttsReadNoFocus(context: Context, langCode: String, text: String): Unit = suspendCoroutine { continuation ->
        //SET UP TTS:
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
                    tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, generateRandomString(8))
                }
            } else {
                Log.e(TAG, "Initialization Failed!")
                continuation.resume(Unit)
            }
        }
    }


    //HELPER: TTS read with audio dimming:
    private fun ttsReadWithFocus(context: Context, audioFocusRequest: AudioFocusRequest, language: String, text: String) {
        //BUILD FOCUS REQUEST:
        val focusRequest = audioManager!!.requestAudioFocus(audioFocusRequest)
        when (focusRequest) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Log.d(TAG, "Cannot gain audio focus! Try again.")
            }

            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                runBlocking {
                    ttsReadNoFocus(context, language, text)
                }
                audioManager!!.abandonAudioFocusRequest(audioFocusRequest)
            }

            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                //mAudioFocusPlaybackDelayed = true
            }
        }
    }

    //TTS READER: MAIN FUNCTION:
    fun ttsRead(context: Context, language: String, text: String, dimAudio: Boolean = false) {
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
                ttsReadWithFocus(context, audioFocusRequest!!, language, text)
            } else {
                runBlocking {
                    ttsReadNoFocus(context, language, text)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ttsRead: cannot read TTS. EXCEPTION: ", e)
        }
    }


    //Release AudioFocus
    fun releaseAudioFocus() {
        try {
            audioManager!!.abandonAudioFocusRequest(audioFocusRequest!!)
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: AudioFocus already released.")
        }
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


    //LOG:
    //Get List of all log files:
    fun getLogKeys(): List<String> {
        var logKeys = mutableListOf<String>()
        var obj = JsonObject()
        if (logDir!!.exists()) {
            var logFiles = logDir!!.list()
            logFiles!!.sortDescending()   //Sort descending by date
            for (f in logFiles) {
                var reader = FileReader(File(logDir, f))
                try {
                    obj = JsonParser.parseReader(reader).asJsonObject
                    //Delete empty searches:
                    if (obj.has("best_score") || obj.has("voc_score")) {
                        val best = if (obj.has("best_score")) obj.get("best_score").asInt else obj.get("voc_score").asInt
                        if (best == 0) {
                            //Delete invalid files:
                            File(logDir, f).delete()
                            Log.w(TAG, "Deleted file: $f")
                        } else {
                            //Add:
                            logKeys.add(f)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ERROR: Cannot open file $f: ", e)
                    //Delete invalid files:
                    File(logDir, f).delete()
                    Log.w(TAG, "Deleted file: $f")
                }
            }
        }
        //Log.d(TAG, logKeys.toString())
        return logKeys
    }


    //Get single log item:
    fun getLogItem(f: String): JsonObject {
        val logItem = JsonObject()
        try {
            var reader = FileReader(File(logDir, f))
            val obj = JsonParser.parseReader(reader).asJsonObject
            //Log.d(TAG, "ITEM: $obj")
            return obj
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot open file $f: ", e)
            return logItem
        }
    }


    //History cleaning:
    fun deleteOldLogs() {
        if (logDir!!.exists()) {
            val thresholdDate = LocalDateTime.now().minusDays(30)
            val thresholdDateStr = thresholdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            Log.d(TAG, "THRESHOLD DATE: $thresholdDateStr")
            var c = 0
            var logFiles = logDir!!.list()
            logFiles!!.sort()   //Sort ascending by date
            //Log.d(TAG, logFiles.toList().toString())
            for (f in logFiles) {
                if (f < thresholdDateStr) {
                    //Log.d(TAG, "Removing file: $f")
                    File(logDir, f).delete()
                    c ++
                } else {
                    break
                }
            }
            Log.d(TAG, "Removed $c older logs.")
        }
    }


    //CLEANING:
    //Clean cached recordings:
    fun cleanOlderRecs(context: Context) {
        try {
            File(context.cacheDir, "$recFileName.mp3").delete()
            Log.d(TAG, "Recording mp3 deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "Recording mp3 not deleted.")
        }
        try {
            File(context.cacheDir, "$recFileName.flac").delete()
            Log.d(TAG, "Recording flac deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "Recording flac not deleted.")
        }
    }


    //GUIDE:
    //Read Guide:
    fun getGuideArray(context: Context): JsonArray {
        var guideArray = JsonArray()
        var reader: BufferedReader? = null
        try {
            //TODO: Add more query languages:
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.guide_eng)))
            //Load:
            guideArray = JsonParser.parseReader(reader).asJsonArray
        } catch (e: Exception) {
            Log.w(TAG, "GUIDE PARSING ERROR: ", e)
        }
        //Log.d(TAG, guideArray.toString())
        return guideArray
    }


    //Get list of Guide items for state:
    fun getGuideStateItems(guideArray: JsonArray): List<String> {
        var guideItems = mutableListOf<String>()
        for (item in guideArray) {
            var itemJson = item.asJsonObject
            var cat = itemJson.get("category").asString
            var requests = itemJson.get("requests").asJsonArray
            for (req in requests) {
                var reqJson = req.asJsonObject
                var intro = reqJson.get("intro").asString
                guideItems.add("$cat - $intro")
            }
        }
        return guideItems
    }


    //On Logout: delete user cache files:
    fun deleteUserCache() {
        try {
            logDir!!.deleteRecursively()
            Log.d(TAG, "Deleted ALL logs.")
            File(libraryDir, "artists").deleteRecursively()
            File(libraryDir, "playlists").deleteRecursively()
            File(libraryDir, "contacts").deleteRecursively()
            Log.d(TAG, "User vocabulary deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "User cache not completely deleted.")
        }
    }

}