package com.ftrono.DJames.utilities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray
import com.google.gson.JsonElement
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
import kotlin.math.min
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


    //Trim strings:
    fun trimString(textOrig: String, maxLength: Int = 30): String {
        var textTrimmed = textOrig
        if (textOrig.length > maxLength) {
            textTrimmed = textOrig.slice(0..maxLength) + "..."
        }
        return textTrimmed
    }

    fun updateItemsCount(filter: String, singularWord: String): String {
        val size = getVocSize(filter)
        if (size == 1) {
            return "1 ${singularWord}"
        } else {
            return "$size ${singularWord}s"
        }
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
    //Get JsonArray out of all log files:
    fun getLogArray(): List<JsonElement> {
        var logArray = JsonArray()
        var obj = JsonObject()
        if (logDir!!.exists()) {
            var logFiles = logDir!!.list()
            logFiles!!.sortDescending()   //Sort descending by date
            for (f in logFiles) {
                var reader = FileReader(File(logDir, f))
                try {
                    obj = JsonParser.parseReader(reader).asJsonObject
                    logArray.add(obj)
                } catch (e: Exception) {
                    //Delete invalid files:
                    File(logDir, f).delete()
                    Log.w(TAG, "Deleted file: $f")
                }
            }
        }
        //Log.d(TAG, logArray.toString())
        return logArray.toList()
    }

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

    //Prepare consolidated Log file:
    fun prepareLogCons(context: Context, hideSuccessful: Boolean): File {
        val logConsName = "requests_log.json"
        val logArray = getLogArray()
        var consFile = File(context.cacheDir, logConsName)
        if (consFile.exists()) {
            consFile.delete()
        }
        consFile.createNewFile()
        consFile.writeText(logArray.toString())
        return consFile
    }

    //VOCABULARY:
    //Get Json with all Vocabulary items for a given "filter" category:
    fun getVocabulary(filter: String): JsonObject {
        /*
        STRUCTURE:
        {
            "item_name_1": {"key_1": "value_1", ...},
            ...
        }
        */
        var vocFile = File(vocDir, "voc_${filter}s.json")
        var vocJson = JsonObject()
        if (!vocFile.exists()) {
            //Create new empty voc file (& return empty array):
            try {
                vocFile.createNewFile()
                vocFile.writeText(vocJson.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Error: voc file not created!", e)
            }
        } else {
            //Read array from existing voc file:
            try {
                var reader = FileReader(vocFile)
                vocJson = JsonParser.parseReader(reader).asJsonObject
            } catch (e: Exception) {
                //Delete invalid file:
                // vocFile.delete()
                Log.w(TAG, "Error in parsing vocabulary file!", e)
            }
        }
        // Log.d(TAG, vocJson.toString())
        return vocJson
    }

    //Get number of Vocabulary items for a given "filter" category:
    fun getVocSize(filter: String): Int {
        var vocFile = File(vocDir, "voc_${filter}s.json")
        var size = 0
        if (vocFile.exists()) {
            //Read array from existing voc file:
            try {
                var reader = FileReader(vocFile)
                var vocJson = JsonParser.parseReader(reader).asJsonObject
                size = vocJson.keySet().size
                //Log.d(TAG, vocJson.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Error in parsing vocabulary file!", e)
            }
        }
        return size
    }

    //Update Vocabulary file:
    fun editVocFile(prevText: String, newText: String = "", newDetails: JsonObject = JsonObject()): Int {
        try {
            //Log.d(TAG, newDetails.toString())
            //Pack JSON:
            var vocFile = File(vocDir, "voc_${filter.value}s.json")
            var reader = FileReader(vocFile)
            var vocJson = JsonParser.parseReader(reader).asJsonObject
            //Remove previous version:
            if (prevText != "" && newText != prevText)  {
                vocJson.remove(prevText)
            }
            //Add new item & details:
            if (newText != "") {
                vocJson.add(newText, newDetails)
            }
            //Sort ascending:
            var keyList = vocJson.keySet().toMutableList()
            keyList.sort()

            //Build new vocabulary (sorted):
            var newJson = JsonObject()
            for (item in keyList) {
                newJson.add(item, vocJson[item].asJsonObject)
            }
            //Log.d(TAG, newJson.toString())

            //Store:
            //Overwrite saved file:
            vocFile.writeText(newJson.toString())
            return 0
        } catch (e: Exception) {
            Log.w(TAG, "Error: vocFile not updated!", e)
            return -1
        }
    }


    //GUIDE:
    //Read Guide:
    fun getGuideArray(context: Context): JsonArray {
        var guideArray = JsonArray()
        var reader: BufferedReader? = null
        try {
            //Get default query language:
//            if (prefs.queryLanguage == "it") {
//                reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.guide_ita)))   //"ita"
//            } else {
//                reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.guide_eng)))   //"eng"
//            }
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.guide_eng)))   //"eng"   //TODO: TEMP!
            //Load:
            guideArray = JsonParser.parseReader(reader).asJsonArray
        } catch (e: Exception) {
            Log.w(TAG, "GUIDE PARSING ERROR: ", e)
        }
        //Log.d(TAG, guideArray.toString())
        return guideArray
    }


    //On Logout: delete user cache files:
    fun deleteUserCache() {
        try {
            logDir!!.deleteRecursively()
            Log.d(TAG, "Deleted ALL logs.")
            File(vocDir, "voc_artists.json").delete()
            File(vocDir, "voc_playlists.json").delete()
            File(vocDir, "voc_contacts.json").delete()
            Log.d(TAG, "User vocabulary deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "User cache not completely deleted.")
        }
    }

}