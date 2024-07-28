package com.ftrono.DJames.utilities

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
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


    fun trimString(textOrig: String, maxLength: Int = 30): String {
        var textTrimmed = textOrig
        if (textOrig.length > maxLength) {
            textTrimmed = textOrig.slice(0..maxLength) + "..."
        }
        return textTrimmed
    }


    //Check Language switch:
    fun checkLanguageSwitch(context: Context, resultsNLP: JsonObject): String {
        var reqLanguage = ""
        //Search items:
        var reader: BufferedReader? = null
        //Calls / message requests -> only use default query language:
        if (prefs.queryLanguage.toInt() == 1) {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_ita)))   //"ita"
        } else {
            reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.match_sents_eng)))   //"eng"
        }
        //Load:
        val sourceSents = JsonParser.parseReader(reader).asJsonObject
        val introLang = sourceSents.get("intro_lang").asJsonArray   //" in "
        var introWords = mutableListOf<String>()
        for (wordEl in introLang) {
            val word = wordEl.asString
            introWords.add(word.strip())
        }

        //text:
        val queryText = resultsNLP.get("query_text").asString
        val tokens = queryText.split(" ")
        //CHECK:
        if (tokens.size > 1) {
            //if language entity found: check if it's the first or second word of the sentence:
            val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.languages)))
            val sourceMap = JsonParser.parseReader(reader).asJsonObject
            for (lang in sourceMap.keySet()) {
                //if the queryText actually includes one of the switch sentences:
                if (tokens[0] == lang || (tokens[1] == lang && tokens[0] in introWords)) {
                    //validate reqLanguage only if different from the default one:
                    if (sourceMap[lang].asString != supportedLanguageCodes[prefs.queryLanguage.toInt()]) {
                        //set reqLanguage to the entity value:
                        reqLanguage = sourceMap[lang].asString
                    }
                    break
                }
            }
        }
        Log.d(TAG, "DETECTED REQLANGUAGE: $reqLanguage")
        return reqLanguage
    }


    //Emoji replacer:
    fun replaceEmojis(context: Context, text: String, reqLanguage: String): String {
        var textReplaced = text
        var reader: BufferedReader? = null
        //Query language:
        var messLanguage = supportedLanguageCodes[prefs.messageLanguage.toInt()]
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
                            if (obj.has("voc_score")) {
                                //Call requests:
                                score = obj.get("voc_score").asInt
                                if (score <= midThreshold) {
                                    logArray.add(obj)
                                }
                            } else {
                                //Play requests:
                                score = obj.get("best_score").asInt
                                if (score <= playThreshold) {
                                    logArray.add(obj)
                                }
                            }
                        } catch (e: Exception) {
                            //Generic requests:
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
        val logArray = getLogArray(hideSuccessful=hideSuccessful)
        var consFile = File(context.cacheDir, logConsName)
        if (consFile.exists()) {
            consFile.delete()
        }
        consFile.createNewFile()
        consFile.writeText(logArray.toString())
        return consFile
    }

    //VOCABULARY:
    //Get Json with all Vocabulary items of a given "filter" category:
    fun getVocabulary(filter: String): JsonObject {
        var vocFile = File(vocDir, "voc_${filter}s.json")
        var vocJson = JsonObject()
        if (!vocFile.exists()) {
            //Create new empty voc file (& return empty array):
            try {
                vocFile.createNewFile()
                vocFile.writeText(vocJson.toString())
            } catch (e: Exception) {
                Log.d(TAG, "Error: voc file not created!")
            }
        } else {
            //Read array from existing voc file:
            try {
                var reader = FileReader(vocFile)
                vocJson = JsonParser.parseReader(reader).asJsonObject
            } catch (e: Exception) {
                //Delete invalid file:
                // vocFile.delete()
                Log.d(TAG, "Error in parsing vocabulary file!", e)
            }
        }
        // Log.d(TAG, vocJson.toString())
        return vocJson
    }

    //Update Vocabulary file:
    fun editVocFile(prevText: String, newText: String = "", newDetails: JsonObject = JsonObject()): Int {
        try {
            //Pack JSON:
            var vocFile = File(vocDir, "voc_${filter}s.json")
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
            // Log.d(TAG, vocJson.toString())

            //Store:
            //Overwrite saved file:
            vocFile.writeText(newJson.toString())
            return 0
        } catch (e: Exception) {
            Log.d(TAG, "Error: vocFile not updated!", e)
            return -1
        }
    }


    //GUIDE:
    //Get JsonArray out of Guide JSON:
    fun getGuideArray(context: Context): JsonArray {
        var guideArray = JsonArray()
        var catJson = JsonObject()
        var sents = JsonArray()
        var obj = JsonObject()
        var sentJson = JsonObject()
        var category: String = ""
        var reader: BufferedReader? = null
        try {
            //Get default query language:
            if (prefs.queryLanguage.toInt() == 1) {
                reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.guide_ita)))   //"ita"
            } else {
                reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.guide_eng)))   //"eng"
            }
            //Load:
            val categories = JsonParser.parseReader(reader).asJsonArray
            for (catRaw in categories) {
                catJson = catRaw.asJsonObject
                //1) Parse header:
                obj = JsonObject()
                category = catJson.get("category").asString
                obj.addProperty("header", catJson.get("header").asString)
                guideArray.add(obj)
                //2) Parse sentences:
                sents = catJson.get("sentences").asJsonArray
                for (sentRaw in sents) {
                    sentJson = sentRaw.asJsonObject
                    //For messages, take only the JSON with the default messageLanguage:
                    if (category != "messages" || sentJson.get("language").asString == supportedLanguageCodes[prefs.messageLanguage.toInt()]){
                        obj = JsonObject()
                        obj.addProperty("intro", sentJson.get("intro").asString)
                        obj.addProperty("sentence_1_intro", sentJson.get("sentence_1_intro").asString)
                        obj.addProperty("sentence_1", sentJson.get("sentence_1").asString)
                        obj.addProperty("sentence_2_intro", sentJson.get("sentence_2_intro").asString)
                        obj.addProperty("sentence_2", sentJson.get("sentence_2").asString)
                        obj.addProperty("description", sentJson.get("description").asString)
                        obj.add("alternatives", sentJson.get("alternatives").asJsonArray)
                        guideArray.add(obj)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "GUIDE PARSING ERROR: ", e)
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
            Log.d(TAG, "User cache not completely deleted.")
        }
    }

}