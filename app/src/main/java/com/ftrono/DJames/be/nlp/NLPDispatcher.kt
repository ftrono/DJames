package com.ftrono.DJames.be.nlp

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.spotify.SpotifyFulfillment
import java.io.File


class NLPDispatcher (private var context: Context) {

    private val TAG = NLPDispatcher::class.java.simpleName

    fun dispatch(
        recFile: File,
        prevDispatch: DispatcherInfo = DispatcherInfo(),
        followUp: Boolean = false,
        messageMode: Boolean = false
    ): DispatcherInfo {
        
        //Init:
        var reqLanguage = prefs.queryLanguage
        var intentName = ""

        if (prevDispatch.reqLanguage != "") {
            reqLanguage = prevDispatch.reqLanguage
        }


        //QUERY NLP:
        var nlpQuery = NLPQuery(context)
        var fulfillment = GenericFulfillment(context)
        var spotify = SpotifyFulfillment(context)
        var resultsNLP = NlpQueryModel()

        //1ST REQUEST -> always in default request language:
        if (!followUp && !messageMode) {
            Log.d(TAG, "1ST REQUEST.")
            resultsNLP = nlpQuery.queryNLP(recFile, messageMode = false, reqLanguage = reqLanguage)

            //Process request:
            if (resultsNLP.intentName != "Fallback") {
                //A) PROCESS:
                try {
                    //Get relevant results:
                    nlp_queryText = resultsNLP.queryText
                    nlp_queryText = fulfillmentUtils.replaceNums(nlp_queryText)
                    intentName = resultsNLP.intentName
                    Log.d(TAG, "NLPDispatcher1: detected intent: $intentName")
                } catch (e: Exception) {
                    Log.w(TAG, "NLPDispatcher1: no NLP results!")
                    return fulfillmentUtils.fallback(saveMessage = false)
                }


                //DISPATCH PROCESSING:
                if (nlp_queryText != "" && intentName != "") {
                    when (intentName) {
                        "CallRequest" -> return fulfillment.contactRequest(resultsNLP)
                        "MessageRequest" -> return fulfillment.contactRequest(resultsNLP)
                        "DriveRequest" -> return fulfillment.driveRequest1(resultsNLP)
                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                        "PlayPodcast" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                        "PlayCollection" -> if (spotifyLoggedIn.value!!) return spotify.playCollection(resultsNLP) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                        "Cancel" -> return fulfillmentUtils.fallback()
                        else -> return fulfillmentUtils.fallback()
                    }
                } else {
                    return fulfillmentUtils.fallback()
                }

            } else {
                //B) EMPTY NLP RESULTS:
                return fulfillmentUtils.fallback()
            }


        //2ND REQUEST:
        } else if (voiceQueryOn) {
            //FOLLOW UP / MESSAGE MODE:
            //Check prev intent & requested language:
            var prevIntent = ""
            var reqLangCode = prevDispatch.reqLanguage

            if (messageMode && prevDispatch.messageType == "voice") {
                //Whatsapp audio message -> no NLP query!
                fulfillmentUtils.saveMessage(
                    type = "user",
                    text = "(recorded voice message)",
                    langCode = resultsNLP.language
                )
                try {
                    Log.d(TAG, "MESSAGE FOLLOWUP: AUDIO MESSAGE.")
                    return fulfillment.sendMessage2(prevDispatch)
                } catch (e: Exception) {
                    Log.w(TAG, "Error in sending Whatsapp audio message!")
                    return fulfillmentUtils.fallback()
                }

            } else {
                //Query NLP:
                if (messageMode) {
                    Log.d(TAG, "MESSAGE FOLLOWUP: TEXT MESSAGE.")
                    resultsNLP = nlpQuery.queryNLP(recFile, messageMode = true, reqLanguage = reqLangCode)

                } else {
                    Log.d(TAG, "GENERIC FOLLOWUP.")
                    //Store previous information:
                    prevIntent = prevDispatch.intentName
                    resultsNLP = nlpQuery.queryNLP(recFile, messageMode = false, reqLanguage = reqLangCode)
                }

                //Process request:
                if (resultsNLP.queryText != "") {
                    //A) PROCESS:
                    try {
                        //Get relevant results:
                        nlp_queryText = resultsNLP.queryText
                        if (messageMode) {
                            fulfillmentUtils.saveMessage(
                                type = "user",
                                text = "(test message hidden)",
                                langCode = resultsNLP.language
                            )
                        } else {
                            nlp_queryText = fulfillmentUtils.replaceNums(nlp_queryText)
                            fulfillmentUtils.saveMessage(
                                type = "user",
                                text = nlp_queryText,
                                langCode = resultsNLP.language
                            )
                        }
                        intentName = resultsNLP.intentName
                        Log.d(TAG, "NLPDispatcher2: detected intent: $intentName")
                    } catch (e: Exception) {
                        Log.w(TAG, "NLPDispatcher2: no NLP results!")
                        return fulfillmentUtils.fallback()
                    }

                    //DISPATCH PROCESSING:
                    if (nlp_queryText != "" && intentName != "") {
                        if (messageMode) {
                            when (intentName) {
                                "Cancel" -> return fulfillmentUtils.fallback()
                                else -> return fulfillment.sendMessage2(prevDispatch)
                            }

                        } else {
                            when (intentName) {
                                "Cancel" -> return fulfillmentUtils.fallback()
                                else -> {
                                    when (prevIntent) {
                                        "DriveRequest" -> return fulfillment.driveRequest2(resultsNLP, prevDispatch)
                                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                                        "PlayPodcast" -> if (spotifyLoggedIn.value!!) return spotify.playPodcast2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback("Not logged in to Spotify!")
                                    }
                                }
                            }
                        }
                    }

                } else {
                    //A) EMPTY NLP RESULTS:
                    return fulfillmentUtils.fallback()
                }
            }
        }
        return fulfillmentUtils.fallback()
    }
    
}