package com.ftrono.DJames.be.nlp

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
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
                    // Update & store user message:
                    lastLog.nlpQueries.add(resultsNLP)
                    fulfillmentUtils.saveLogMessage(
                        type = "user",
                        text = nlp_queryText,
                        langCode = resultsNLP.language
                    )
                    Log.d(TAG, "NLPDispatcher1: detected intent: $intentName")

                } catch (e: Exception) {
                    Log.w(TAG, "NLPDispatcher1: no NLP results!")
                    return fulfillmentUtils.fallback()   //Error
                }


                //DISPATCH PROCESSING:
                if (nlp_queryText != "" && intentName != "") {
                    when (intentName) {
                        "CallRequest" -> if (utils.checkPermission(context, Manifest.permission.CALL_PHONE)) return fulfillment.contactRequest(resultsNLP) else return fulfillmentUtils.fallback(noPermission=true)
                        "MessageRequest" -> if (utils.checkPermission(context, Manifest.permission.SEND_SMS)) return fulfillment.contactRequest(resultsNLP) else return fulfillmentUtils.fallback(noPermission=true)
                        "DriveRequest" -> return fulfillment.driveRequest1(resultsNLP)
                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayPodcast" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayCollection" -> if (spotifyLoggedIn.value!!) return spotify.playCollection(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "Cancel" -> return fulfillmentUtils.fallback(nevermind=true)
                        else -> return fulfillmentUtils.fallback(notUnderstood=true)
                    }
                } else {
                    return fulfillmentUtils.fallback(notUnderstood=true)
                }

            } else {
                //B) EMPTY NLP RESULTS:
                return fulfillmentUtils.fallback()   //Error
            }


        //2ND REQUEST:
        } else if (voiceQueryOn) {
            //FOLLOW UP / MESSAGE MODE:
            //Check prev intent & requested language:
            var prevIntent = ""
            var reqLangCode = prevDispatch.reqLanguage

            if (messageMode && prevDispatch.messageType == "voice") {
                //Whatsapp audio message -> no NLP query!
                fulfillmentUtils.saveLogMessage(
                    type = "user",
                    text = "(recorded voice message)",
                    langCode = resultsNLP.language
                )
                try {
                    Log.d(TAG, "MESSAGE FOLLOWUP: AUDIO MESSAGE.")
                    return fulfillment.sendMessage2(prevDispatch)
                } catch (e: Exception) {
                    Log.w(TAG, "Error in sending Whatsapp audio message!")
                    return fulfillmentUtils.fallback()   //Error
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
                    var storedText = ""
                    try {
                        //Get relevant results:
                        intentName = resultsNLP.intentName
                        if (messageMode) {
                            // Anonymize:
                            storedText = "(message text hidden)"
                        } else {
                            // Store fully:
                            nlp_queryText = fulfillmentUtils.replaceNums(resultsNLP.queryText)
                            storedText = nlp_queryText
                            lastLog.nlpQueries.add(resultsNLP)
                        }
                        // Update & store user Message:
                        fulfillmentUtils.saveLogMessage(
                            type = "user",
                            text = storedText,
                            langCode = resultsNLP.language
                        )
                        Log.d(TAG, "NLPDispatcher2: detected intent: $intentName")
                    } catch (e: Exception) {
                        Log.w(TAG, "NLPDispatcher2: no NLP results!")
                        return fulfillmentUtils.fallback()   //Error
                    }

                    //DISPATCH PROCESSING:
                    if (nlp_queryText != "" && intentName != "") {
                        if (messageMode) {
                            when (intentName) {
                                "Cancel" -> return fulfillmentUtils.fallback(nevermind=true)
                                else -> return fulfillment.sendMessage2(prevDispatch)
                            }

                        } else {
                            when (intentName) {
                                "Cancel" -> return fulfillmentUtils.fallback(nevermind=true)
                                else -> {
                                    when (prevIntent) {
                                        "DriveRequest" -> return fulfillment.driveRequest2(resultsNLP, prevDispatch)
                                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayPodcast" -> if (spotifyLoggedIn.value!!) return spotify.playPodcast2(resultsNLP, prevDispatch) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                    }
                                }
                            }
                        }
                    }

                } else {
                    //A) EMPTY NLP RESULTS:
                    return fulfillmentUtils.fallback()   //Error
                }
            }
        }
        return fulfillmentUtils.fallback()   //Error
    }
    
}