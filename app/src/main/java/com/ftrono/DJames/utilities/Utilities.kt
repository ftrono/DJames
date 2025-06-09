package com.ftrono.DJames.utilities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.telephony.PhoneNumberUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.ftrono.DJames.be.models.HttpResponse
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.streams.asSequence
import androidx.core.net.toUri
import com.ftrono.DJames.be.database.HistoryLog
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.models.languageNamesMap
import com.ftrono.DJames.be.models.languageWordsMap
import java.util.Locale


class Utilities {
    private val TAG = Utilities::class.java.simpleName

    //OkHTTP: make HTTP request:
    suspend fun makeRequest(client: OkHttpClient, request: Request): HttpResponse = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(
                    HttpResponse(
                        code = response.code,
                        body = response.body!!.string()
                    )
                )
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(
                    HttpResponse(
                        code = -1,  // -1 to indicate failure
                        body = ""
                    )
                )
                Log.d("TAG", "RESPONSE ERROR: ", e)
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


    //Clean string keeping only alphanumeric characters:
    fun cleanString(text: String): String {
        //val re = Regex("[^A-Za-z0-9 ]")
        val re = Regex("[\\p{P}\\p{S}]|[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        val cleaned = re.replace(text, " ").replace(Regex("\\s+"), " ").trim()
        return capitalizeWords(cleaned).trim()
    }


    //Check if string is made of alphabetic characters:
    fun isLetters(string: String): Boolean {
        return string.all { it.isLetter() }
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


    //Open link externally:
    fun openLink(context: Context, url: String, fromService: Boolean = false) {
        //Intent view:
        Log.d(TAG, "Opening URL: $url")
        val intent = Intent(
            Intent.ACTION_VIEW,
            url.toUri()
        )
        if (fromService) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("fromwhere", "ser")
        }
        context.startActivity(intent)
    }


    //UI update:
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
    fun getLanguageCode(language: String, default: String = ""): String {
        var reqLangCode = default
        if (languageWordsMap.contains(language)) {
            reqLangCode = languageWordsMap[language]!!
            Log.d(TAG, "REQUESTED LANGUAGE: $reqLangCode")
        } else {
            Log.d(TAG, "No requested language found in the voice request.")
        }
        return reqLangCode
    }


    //Get the corresponding language name in preferred language for the detected language code:
    fun getLanguageName(languageCode: String): String {
        return languageNamesMap[prefs.queryLanguage]!![languageCode]!!
    }


    fun stopThreadsInList(threads: MutableList<Thread>) {
        //Stop threads:
        var t_count = 1
        var t_max = threads.size
        for (t in threads) {
            try {
                if (t.isAlive()) {
                    t.interrupt()
                    Log.d(TAG, "Stopped thread $t_count / $t_max.")
                }
                Log.d(TAG, "Thread $t_count / $t_max not active.")
            } catch (e: Exception) {
                Log.w(TAG, "Thread $t_count / $t_max: EXCEPTION: ", e)
            }
            t_count ++
        }
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
    fun deleteUserCache(context: Context) {
        try {
            logDir!!.deleteRecursively()
            Log.d(TAG, "Deleted ALL logs.")
            libUtils.deleteLibrary(context, "artists")
            libUtils.deleteLibrary(context, "playlists")
            libUtils.deleteLibrary(context, "contacts")
            Log.d(TAG, "User library deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "User cache not completely deleted.")
        }
    }

}