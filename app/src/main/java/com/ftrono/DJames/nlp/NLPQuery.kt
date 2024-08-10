package com.ftrono.DJames.nlp

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.*
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.dialogflow.v2.AudioEncoding
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.InputAudioConfig
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.QueryResult
import com.google.cloud.dialogflow.v2.SessionName
import com.google.cloud.dialogflow.v2.SessionsClient
import com.google.cloud.dialogflow.v2.SessionsSettings
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonArray
import com.google.protobuf.ByteString
import java.io.File


class NLPQuery(context: Context) {
    private val TAG = NLPQuery::class.java.simpleName
    private var sessionId: SessionName? = null
    private var sessionsClient: SessionsClient? = null

    init {
        try {
            val stream = context.resources.openRawResource(R.raw.nlp_credentials)
            val credentials = GoogleCredentials.fromStream(stream)
            val settingsBuilder = SessionsSettings.newBuilder()
            val sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            sessionId = SessionName.of(dialogflow_id, prefs.nlpUserId)
            Log.d(TAG, "nlpUserId: ${prefs.nlpUserId}")
            Log.d(TAG, "CREATED: sessionsClient: ${sessionsClient}")
            Log.d(TAG, "CREATED: sessionId: ${sessionId}")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: NLP Session not created!", e)
        }
    }


    //Service status checker:
    val sessionThread = Thread {
        try {
            synchronized(this) {
                try {
                    Thread.sleep(queryTimeout.toLong() * 1000)   //default: 5
                    sessionsClient!!.shutdown()
                    Log.d(TAG, "Connection Error: sessionsClient manually SHUT DOWN.")
                } catch (e: InterruptedException) {
                    Log.d(TAG, "NLPQuery: sessionThread already off.")
                }
            }
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted: exception.", e)
        }
    }


    fun queryNLP(recFile: File, messageMode: Boolean = false, reqLanguage: String = ""): JsonObject {
        var respJson = JsonObject()
        try {
            var languageCode = ""
            var punct = false
            //punctuation:
            if (messageMode) {
                punct = true
            }
            //requested language:
            if (reqLanguage != "") {
                languageCode = reqLanguage
            } else if (messageMode) {
                languageCode = supportedLanguageCodes[prefs.messageLanguage.toInt()]
            } else {
                languageCode = supportedLanguageCodes[prefs.queryLanguage.toInt()]
            }
            val inputAudioConfig: InputAudioConfig = InputAudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_FLAC)
                .setSampleRateHertz(recSamplingRate)
                .setLanguageCode(languageCode)
                .setModel("latest_short")
                .setSingleUtterance(false)
                .setEnableAutomaticPunctuation(punct)
                .build()

            val queryInput: QueryInput = QueryInput.newBuilder()
                .setAudioConfig(inputAudioConfig)
                .build()

            val detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(sessionId.toString())
                .setQueryInput(queryInput)
                .setInputAudio(ByteString.copyFrom(recFile.readBytes()))
                .build()

            //Log.d(TAG, "SENT detectIntentRequest REQUEST: ${JsonFormat.printer().print(detectIntentRequest)}")

            //Run NLP query with handmade timeout:
            if (!sessionThread.isAlive()) {
                sessionThread.start()
            }
            val responseObj = sessionsClient!!.detectIntent(detectIntentRequest)
            if (sessionThread.isAlive()) {
                sessionThread.interrupt()
            }
            val queryResult: QueryResult = responseObj.queryResult
            //Log.d(TAG, "SUCCESS detectIntentRequest RESPONSE: ${JsonFormat.printer().print(responseObj)}")
            Log.d(TAG, "SUCCESS detectIntentRequest RESPONSE: ${queryResult}")

            try {
                //EXTRACT NLP RESULTS:
                //NLP basic results:
                var languageFound = queryResult.languageCode
                var queryText = queryResult.queryText
                if (!messageMode) {
                    queryText = queryText.lowercase()
                }
                //Key information:
                respJson.addProperty("request_language", languageFound)
                respJson.addProperty("query_text", queryText)
                respJson.addProperty("intent_name", queryResult.intent.displayName)
                //Artists:
                try {
                    var artistsList = JsonArray()
                    try {
                        //Multiple artists:
                        var artistsObj = queryResult.parameters.fieldsMap["music-artist"]!!.listValue.valuesList
                        for (artist in artistsObj) {
                            artistsList.add(artist.stringValue)
                        }
                    } catch (e: Exception) {
                        //Single artist:
                        artistsList.add(queryResult.parameters.fieldsMap["music-artist"]!!.stringValue)
                    }
                    //LIST:
                    respJson.add("artists", artistsList)
                } catch (e: Exception) {
                    //No artist:
                    respJson.add("artists", JsonArray())
                }
                //Genre:
                try {
                    respJson.addProperty("genre", queryResult.parameters.fieldsMap["music-genre"]!!.stringValue.lowercase())
                } catch (e: Exception) {
                    respJson.addProperty("genre", "")
                }
                //Language:
                try {
                    respJson.addProperty("reqLanguage", queryResult.parameters.fieldsMap["language"]!!.stringValue.lowercase())
                } catch (e: Exception) {
                    respJson.addProperty("reqLanguage", "")
                }

                Log.d(TAG, "SUCCESS detectIntentResponse RESPONSE: ${respJson}")

            } catch (e: Exception) {
                //Response is not a JSON:
                Log.w(TAG, "ERROR DetectIntentResponse: cannot interpret the response received. ", e)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR DetectIntentRequest: ", e)
        }
        try {
            sessionsClient!!.shutdown()
        } catch (e: Exception) {
            Log.d(TAG, "SessionsClient already shut down.")
        }
        return respJson
    }
    
}