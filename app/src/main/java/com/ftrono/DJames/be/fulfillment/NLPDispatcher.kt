package com.ftrono.DJames.be.fulfillment

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultChatWait
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.agents.data.StateInfo
import java.io.File


class NLPDispatcher (
    private var context: Context
) {
    private val TAG = this::class.java.simpleName

    fun dispatch(
        text: String = "",
        recFile: File? = null,
        prevState: StateInfo = StateInfo(),
        isStart: Boolean = false,
        fromVoice: Boolean = false,
    ): StateInfo {

        //Init:
        var reqLanguage = prefs.queryLanguage
        var intentName = ""
        var messageMode = prevState.messageMode

        if (prevState.reqLanguage != "") {
            reqLanguage = prevState.reqLanguage
        }


        //QUERY NLP:
        var nlpQuery = NLPQuery(context)
        var fulfillment = GenericFulfillment(context)
        var spotify = SpotifyFulfillment(context)
        var resultsNLP = NlpQueryModel()

        //1ST REQUEST -> always in default request language:
        if (isStart) {
            Log.d(TAG, "1ST REQUEST.")
            resultsNLP = nlpQuery.queryNLP(text=text, recFile=recFile, messageMode = false, reqLanguage = reqLanguage)

            //Process request:
            if (resultsNLP.queryText != "") {
                //A) PROCESS:
                try {
                    //Get relevant results:
                    nlp_queryText = resultsNLP.queryText
                    nlp_queryText = fulfillmentUtils.replaceNums(nlp_queryText)
                    intentName = resultsNLP.intentName
                    lastRequestIntent = intentName
                    // Update & store user message:
                    lastUserMessage.text = nlp_queryText
                    lastUserMessage.requestIntent = intentName
                    lastUserMessage.attachments.nlpQueries = mutableListOf<NlpQueryModel>()
                    lastUserMessage.attachments.nlpQueries!!.add(resultsNLP)   // TODO: TEMP
                    messageUtils.storeMessage(
                        context = context,
                        langCode = resultsNLP.language,
                        fromUser = true,
                        fromVoice = fromVoice,
                        isStart = true
                    )
                    Log.d(TAG, "NLPDispatcher1: detected intent: $intentName")

                } catch (e: Exception) {
                    //Error:
                    Log.w(TAG, "NLPDispatcher1: no NLP results!")
                    return fulfillmentUtils.fallback(
                        noSave = true   // Don't save first fallback
                    )
                }

                // Typing delay:
                if (text != "") {
                    Thread.sleep(defaultChatWait)
                }

                //DISPATCH PROCESSING:
                if (nlp_queryText != "" && intentName != "") {
                    when (intentName) {
                        "CallRequest" -> if (utils.checkPermission(context, Manifest.permission.CALL_PHONE)) return fulfillment.contactRequest(resultsNLP, fromVoice=fromVoice) else return fulfillmentUtils.fallback(noPermission=true)
                        "MessageRequest" -> if (utils.checkPermission(context, Manifest.permission.SEND_SMS)) return fulfillment.contactRequest(resultsNLP, fromVoice=fromVoice) else return fulfillmentUtils.fallback(noPermission=true)
                        "DriveRequest" -> return fulfillment.driveRequest1(resultsNLP)
                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayPodcast" -> if (spotifyLoggedIn.value!!) return spotify.playItem1(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "PlayCollection" -> if (spotifyLoggedIn.value!!) return spotify.playCollection(resultsNLP) else return fulfillmentUtils.fallback(notLoggedIn=true)
                        "TestAgents" -> return fulfillmentUtils.fallback(notAvailable=true)   // TODO: TEMP
                        "Cancel" -> return fulfillmentUtils.fallback(nevermind=true)
                        else -> return fulfillmentUtils.fallback(notUnderstood=true)
                    }
                } else {
                    return fulfillmentUtils.fallback(notUnderstood=true)
                }

            } else {
                //B) EMPTY NLP RESULTS:
                return fulfillmentUtils.fallback(
                    notUnderstood=true,
                    noSave = true,   // Don't save first fallback
                )
            }


        //2ND REQUEST:
        } else {
            //FOLLOW UP / MESSAGE MODE:
            //Check prev intent & requested language:
            var prevIntent = ""
            var reqLangCode = prevState.reqLanguage

            if (messageMode && prevState.messageType == "voice" && fromVoice) {
                Log.d(TAG, "MESSAGE FOLLOWUP: AUDIO MESSAGE.")
                //Whatsapp audio message -> no NLP query!
                val storedText = "(private voice message)"
                lastUserMessage.text = storedText
                lastUserMessage.requestIntent = lastRequestIntent
                messageUtils.storeMessage(
                    context = context,
                    langCode = prefs.queryLanguage,
                    fromUser = true,
                    fromVoice = true
                )
                try {
                    return fulfillment.sendMessage2(prevState)
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
                    prevIntent = prevState.intentName
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

                        } else {
                            // Store fully:
                            storedText = nlp_queryText
                            lastUserMessage.attachments.nlpQueries = mutableListOf<NlpQueryModel>()
                            lastUserMessage.attachments.nlpQueries!!.add(resultsNLP)   //TODO: TEMP
                        }
                        // Update & store user Message:
                        lastUserMessage.text = storedText
                        lastUserMessage.requestIntent = lastRequestIntent
                        messageUtils.storeMessage(
                            context = context,
                            langCode = resultsNLP.language,
                            fromUser = true,
                            fromVoice = fromVoice
                        )
                        Log.d(TAG, "NLPDispatcher2: detected intent: $intentName")
                    } catch (e: Exception) {
                        Log.w(TAG, "NLPDispatcher2: no NLP results!")
                        return fulfillmentUtils.fallback()   //Error
                    }

                    // Typing delay:
                    if (!fromVoice) {
                        Thread.sleep(defaultChatWait)
                    }

                    //DISPATCH PROCESSING:
                    if (nlp_queryText != "" && intentName != "") {
                        if (messageMode) {
                            when (intentName) {
                                "Cancel" -> return fulfillmentUtils.fallback(nevermind = true)
                                else -> return fulfillment.sendMessage2(prevState)
                            }
                        } else {
                            when (intentName) {
                                "Cancel" -> return fulfillmentUtils.fallback(nevermind=true)
                                else -> {
                                    when (prevIntent) {
                                        "DriveRequest" -> return fulfillment.driveRequest2(resultsNLP, prevState)
                                        "PlaySong" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevState) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayAlbum" -> if (spotifyLoggedIn.value!!) return spotify.playSongAlbum2(resultsNLP, prevState) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayArtist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevState) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayPlaylist" -> if (spotifyLoggedIn.value!!) return spotify.playArtistPlaylist2(resultsNLP, prevState) else return fulfillmentUtils.fallback(notLoggedIn=true)
                                        "PlayPodcast" -> if (spotifyLoggedIn.value!!) return spotify.playPodcast2(resultsNLP, prevState) else return fulfillmentUtils.fallback(notLoggedIn=true)
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
