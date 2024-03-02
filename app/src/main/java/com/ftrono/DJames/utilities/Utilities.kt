package com.ftrono.DJames.utilities

import android.content.Context
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.streams.asSequence
import com.ftrono.DJames.application.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import com.google.gson.JsonParser
import java.io.FileReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


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


    //LOG:
    //Get JsonArray out of all log files:
    fun getLogArray(hideSuccessful: Boolean): JsonArray {
        var logArray = JsonArray()
        var obj = JsonObject()
        var score = 0
        if (logDir!!.exists()) {
            var logFiles = logDir!!.list()
            logFiles!!.sortDescending()   //Sort descending by date
            for (f in logFiles) {
                var reader = FileReader(File(logDir, f))
                //Delete invalid files:
                try {
                    obj = JsonParser.parseReader(reader).asJsonObject
                    if (!hideSuccessful) {
                        //Add all:
                        logArray.add(obj)
                    } else {
                        //Add doubtful only:
                        try {
                            score = obj.get("best_score").asInt
                            if (score <= matchDoubleThreshold){
                                logArray.add(obj)
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "File $f: No score.")
                        }
                    }
                } catch (e: Exception) {
                    File(logDir, f).delete()
                    Log.d(TAG, "Deleted file: $f")
                }
            }
        }
        //Log.d(TAG, logArray.toString())
        return logArray
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
        val logArray = utils.getLogArray(hideSuccessful=hideSuccessful)
        var consFile = File(context.cacheDir, logConsName)
        if (consFile.exists()) {
            consFile.delete()
        }
        consFile.createNewFile()
        consFile.writeText(logArray.toString())
        return consFile
    }

    //VOCABULARY:
    //Get Json with all Vocabulary files:
    fun getVocabularyArray(filter: String = "", newItem: Boolean = false): JsonArray {
        var vocArray = JsonArray()
        if (vocDir!!.exists()) {
            var vocFiles = vocDir!!.list()
            vocFiles!!.sort()   //Sort ascending
            if (newItem) {
                var newObj = JsonObject()
                newObj.addProperty("item_type", filter)
                newObj.addProperty("item_text", "")
                vocArray.add(newObj)
            }
            for (f in vocFiles) {
                Log.d(TAG, f)
                try {
                    if (filter == "" || f.split("_")[0].lowercase() == filter) {
                        //Add to array:
                        var reader = FileReader(File(vocDir, f))
                        vocArray.add(JsonParser.parseReader(reader).asJsonObject)
                    }
                } catch (e: Exception) {
                    //Delete invalid files:
                    File(vocDir, f).delete()
                    Log.d(TAG, "Deleted file: $f")
                }
            }
        }
        //Log.d(TAG, vocArray.toString())
        return vocArray
    }

}