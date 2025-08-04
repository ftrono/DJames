package com.ftrono.DJames.be.nlp

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultChatWait
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.spotify.SpotifyFulfillment
import java.io.File


class NLPDispatcher (private var context: Context) {

    private val TAG = NLPDispatcher::class.java.simpleName

    fun dispatch(
        text: String = "",
        recFile: File? = null,
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
            resultsNLP = nlpQuery.queryNLP(text=text, recFile=recFile, messageMode = false, reqLanguage = reqLanguage)

            //Process request:
            if (resultsNLP.queryText != "") {
                //A) PROCESS:
                try {
                    //Get relevant results:
                    messageUtils.createMessage(fromUser = true)
                    nlp_queryText = resultsNLP.queryText
                    nlp_queryText = fulfillmentUtils.replaceNums(nlp_queryText)
                    intentName = resultsNLP.intentName
                    // Update & store user message:
                    lastUserMessage.text = nlp_queryText
                    lastUserMessage.langCode = resultsNLP.language
                    lastUserMessage.requestIntent = intentName
                    lastRequestIntent = intentName
                    lastAiMessage.attachments.nlpQueries.add(resultsNLP)   // TODO: TEMP
                    messageUtils.storeMessage(context, fromUser = true, fromVoice = text=="")
                    Log.d(TAG, "NLPDispatcher1: detected intent: $intentName")

                } catch (e: Exception) {
                    Log.w(TAG, "NLPDispatcher1: no NLP results!")
                    return fulfillmentUtils.fallback()   //Error
                }

                // Typing delay:
                if (text != "") {
                    Thread.sleep(defaultChatWait)
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
                return fulfillmentUtils.fallback(notUnderstood=true)
            }


        //2ND REQUEST:
        } else {
            //FOLLOW UP / MESSAGE MODE:
            //Check prev intent & requested language:
            var prevIntent = ""
            var reqLangCode = prevDispatch.reqLanguage

            if (messageMode && prevDispatch.messageType == "voice" && text == "") {
                //Whatsapp audio message -> no NLP query!
                val storedText = "(private voice message)"
                lastUserMessage.text = storedText
                lastUserMessage.requestIntent = lastRequestIntent
                messageUtils.storeMessage(context, fromUser = true, fromVoice = true)
                try {
                    Log.d(TAG, "MESSAGE FOLLOWUP: AUDIO MESSAGE.")
                    lastUserMessage.text = storedText
                    lastUserMessage.requestIntent = lastRequestIntent
                    messageUtils.storeMessage(context, fromUser = true, fromVoice = true)
                    return fulfillment.sendMessage2(prevDispatch)
                } catch (e: Exception) {
                    Log.w(TAG, "Error in sending Whatsapp audio message!")
                    return fulfillmentUtils.fallback()   //Error
                }

            } else {
                //Query NLP:
                if (messageMode) {
                    Log.d(TAG, "MESSAGE FOLLOWUP: TEXT MESSAGE.")
                    resultsNLP = nlpQuery.queryNLP(text=text, recFile=recFile, messageMode = true, reqLanguage = reqLangCode)

                } else {
                    Log.d(TAG, "GENERIC FOLLOWUP.")
                    //Store previous information:
                    prevIntent = prevDispatch.intentName
                    resultsNLP = nlpQuery.queryNLP(text=text, recFile=recFile, messageMode = false, reqLanguage = reqLangCode)
                }

                //Process request:
                if (resultsNLP.queryText != "") {
                    //A) PROCESS:
                    var storedText = ""
                    try {
                        //Get relevant results:
                        intentName = resultsNLP.intentName
                        nlp_queryText = fulfillmentUtils.replaceNums(resultsNLP.queryText)
                        if (messageMode) {
                            // Anonymize:
                            storedText = "(private message text)"
                            lastUserMessage.text = storedText
                        } else {
                            // Store fully:
                            storedText = nlp_queryText
                            lastAiMessage.attachments.nlpQueries.add(resultsNLP)   //TODO: TEMP
                        }
                        // Update & store user Message:
                        lastUserMessage.text = storedText
                        lastUserMessage.langCode = resultsNLP.language
                        lastUserMessage.requestIntent = lastRequestIntent
                        messageUtils.storeMessage(context, fromUser = true, fromVoice = text=="")
                        Log.d(TAG, "NLPDispatcher2: detected intent: $intentName")
                    } catch (e: Exception) {
                        Log.w(TAG, "NLPDispatcher2: no NLP results!")
                        return fulfillmentUtils.fallback()   //Error
                    }

                    // Typing delay:
                    if (text != "") {
                        Thread.sleep(defaultChatWait)
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
                    return fulfillmentUtils.fallback(notUnderstood=true)
                }
            }
        }
        return fulfillmentUtils.fallback()   //Error
    }

}