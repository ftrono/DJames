package com.ftrono.DJames.nlp

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.newsTalk
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.spotify.SpotifyFulfillment
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import java.io.File


class NLPDispatcher (private var context: Context) {

    private val TAG = NLPDispatcher::class.java.simpleName
    private val utils = Utilities()

    fun dispatch(recFile: File, prevStatus: JsonObject = JsonObject(), followUp: Boolean = false, messageMode: Boolean = false) : JsonObject {
        /*
        {
            "fail": Bool (standalone final),
            "stopService": Bool (standalone final),
            "stopSound": Bool (only if true),
            "followUp": Bool (only if true),
            "messageMode": Bool (only if true),
            "toastText": String (only if needed),
            "reqLanguage": String,
            "intent_name": String,
            "play_type": String,
            "context_type": String
        }
         */

        //Init:
        var reqLanguage = prefs.queryLanguage
        var intentName = ""

        if (prevStatus.has("reqLanguage")) {
            reqLanguage = prevStatus.get("reqLanguage").asString
        }


        //QUERY NLP:
        var nlpQuery = NLPQuery(context)
        var fulfillment = GenericFulfillment(context)
        var spotify = SpotifyFulfillment(context)
        var resultsNLP = JsonObject()

        //1ST REQUEST -> always in default request language:
        if (!followUp && !messageMode) {
            Log.d(TAG, "1ST REQUEST.")
            resultsNLP = nlpQuery.queryNLP(recFile, messageMode = false, reqLanguage = reqLanguage)

            //Process request:
            if (resultsNLP.isEmpty) {
                //A) EMPTY NLP RESULTS:
                return utils.fallback()

            } else {
                //B) PROCESS:
                try {
                    //Get relevant results:
                    nlp_queryText = resultsNLP.get("query_text").asString
                    intentName = resultsNLP.get("intent_name").asString
                    Log.d(TAG, "NLPDispatcher1: detected intent: $intentName")
                } catch (e: Exception) {
                    Log.w(TAG, "NLPDispatcher1: no NLP results!")
                    return utils.fallback()
                }


                //DISPATCH PROCESSING:
                if (nlp_queryText != "" && intentName != "") {
                    when (intentName) {
                        "CallRequest" -> return fulfillment.makeCall(resultsNLP)
                        "MessageRequest" -> return fulfillment.sendMessage1(resultsNLP)
                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return utils.fallback("Not logged in to Spotify!")
                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return utils.fallback("Not logged in to Spotify!")
                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return utils.fallback("Not logged in to Spotify!")
                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return utils.fallback("Not logged in to Spotify!")
                        "PlayCollection" -> if (spotifyLoggedIn.value!!) return spotify.playCollection(resultsNLP) else return utils.fallback("Not logged in to Spotify!")
                        "Cancel" -> return utils.fallback()
                        else -> return utils.fallback("Sorry, I did not understand!")
                    }
                } else {
                    return utils.fallback("Sorry, I did not understand!")
                }
            }


        //2ND REQUEST:
        } else if (voiceQueryOn) {
            //FOLLOW UP / MESSAGE MODE:
            //Check prev intent & requested language:
            var prevIntent = ""
            var reqLangCode = prevStatus.get("reqLanguage").asString

            //Query NLP:
            if (messageMode) {
                Log.d(TAG, "MESSAGE FOLLOWUP.")
                resultsNLP =
                    nlpQuery.queryNLP(recFile, messageMode = true, reqLanguage = reqLangCode)

            } else {
                Log.d(TAG, "GENERIC FOLLOWUP.")
                //Store previous information:
                prevIntent = prevStatus.get("intent_name").asString
                if (!newsTalk) {
                    last_log!!.addProperty("query_text", nlp_queryText)
                }
                resultsNLP =
                    nlpQuery.queryNLP(recFile, messageMode = false, reqLanguage = reqLangCode)
            }

            //Process request:
            if (resultsNLP.isEmpty) {
                //A) EMPTY NLP RESULTS:
                return utils.fallback()

            } else {
                //B) PROCESS:
                try {
                    //Get relevant results:
                    nlp_queryText = resultsNLP.get("query_text").asString
                    intentName = resultsNLP.get("intent_name").asString
                    Log.d(TAG, "NLPDispatcher2: detected intent: $intentName")
                } catch (e: Exception) {
                    Log.w(TAG, "NLPDispatcher2: no NLP results!")
                    return utils.fallback()
                }

                //DISPATCH PROCESSING:
                if (nlp_queryText != "" && intentName != "") {
                    if (messageMode) {
                        when (intentName) {
                            "Cancel" -> return utils.fallback()
                            else -> return fulfillment.sendMessage2(prevStatus)
                        }

                    } else {
                        when (intentName) {
                            "Cancel" -> return utils.fallback()
                            else -> {
                                when (prevIntent) {
                                    "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevStatus) else return utils.fallback("Not logged in to Spotify!")
                                    "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevStatus) else return utils.fallback("Not logged in to Spotify!")
                                    "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevStatus) else return utils.fallback("Not logged in to Spotify!")
                                    "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevStatus) else return utils.fallback("Not logged in to Spotify!")
                                }
                            }
                        }
                    }
                }
            }
        }
        return utils.fallback()
    }
    
}