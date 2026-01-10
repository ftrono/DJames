package com.ftrono.DJames.be.agents.fulfillment

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.messLangLower
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.agents.data.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo


class GenericFulfillment (private var context: Context) {
    private val TAG = this::class.java.simpleName


    //Process a request involving a Contact (Call / Message):
    fun contactRequest(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState
        var queryText = updState.messages.last().content
        val filter = "contact"
        var itemInfo = LibraryItem(
            type = filter
        )

        //Extract contact:
        var extractorInfo = ExtractorInfo()
        var nlpExtractor = NLPExtractor(context)
        var contactExtracted = nlpExtractor.extractContact(queryText)
        extractorInfo.matchExtracted = contactExtracted

        //Match extracted contact name with user library:
        var libMatch = libUtils.matchLibrary(filter, text=contactExtracted)

        if (libMatch.matchId < 0) {
            //Fallback:
            return fulfillmentUtils.fallback(updState, notUnderstood=true)

        } else {
            //Get contact:
            itemInfo = libUtils.getLibItemById(libMatch.matchId)
            extractorInfo.matchConfirmed = itemInfo.name

            //CASES:
            if (updState.intentName.contains("Call")) {
                //A) CALL:
                //Reply:
                val ttsToRead = defaultReplies.replyCalling(itemInfo.name)
                val aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = ttsToRead
                    )
                )

                //updState:
                updState.aiReplies = aiReplies
                updState.actionType = ActionType.CALL
                updState.playAcknowledge = true
                updState.attachments.usable = itemInfo

            } else if (updState.intentName.contains("Message")) {
                //B) MESSAGE:
                //Check if voice request contains a specific requested language:
                var reqLangName = updState.reqLangName
                var reqLangCode = updState.reqLangCode

                //If no specific language requested -> use contact preferences or global preferences:
                if (reqLangCode == prefs.messageLanguage) {
                    try {
                        //Contact preferences:
                        reqLangCode = itemInfo.language
                        reqLangName = messLangLower[messLangCodes.indexOf(reqLangCode)]
                        Log.d(TAG, "PREFERRED CONTACT LANGUAGE: $reqLangCode")
                    } catch (e: Exception) {
                        //Global preferences:
                        reqLangCode = prefs.messageLanguage
                        reqLangName = messLangFull[messLangCodes.indexOf(prefs.messageLanguage)]
                        Log.d(TAG, "Messaging in default language: $reqLangCode")
                    }
                }
                extractorInfo.reqLanguage = reqLangCode

                //Extract message type:
                updState.messageType = nlpExtractor.extractMessageType(queryText)
                // Select action to take:
                if (updState.messageType == "voice") {
                    updState.actionType = ActionType.WA_VOICE
                    if (!updState.fromVoice) {
                        //Fallback:
                        return fulfillmentUtils.fallback(updState, cannotRecordWAVoice=true)
                    }
                } else if (updState.messageType == "whatsapp") {
                    updState.actionType = ActionType.WA_TEXT
                } else {
                    updState.actionType = ActionType.SMS
                }

                //Reply:
                var ttsToRead = if (updState.messageType == "voice") {
                    defaultReplies.replyMessageRecord(itemInfo.name)
                } else {
                    defaultReplies.replyMessageDictate(itemInfo.name, updState.messageType, reqLangName)
                }
                val aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = ttsToRead
                    )
                )

                //updState:
                updState.interrupt = true   // Messages only!
                updState.aiReplies = aiReplies
                updState.messageMode = true
                updState.reqLangCode = reqLangCode   // Updated as requested
                updState.attachments.usable = itemInfo

            } else {
                //Fallback:
                return fulfillmentUtils.fallback(updState, notUnderstood=true)
            }

            // Update state:
            updState.attachments.nlpExtractor = extractorInfo
            updState.attachments.matchScore = libMatch.matchScore
            Log.d(TAG, updState.toString())
            return updState
        }
    }


    //Send a message: PART 2:
    fun sendMessage2(prevState: StateInfo): StateInfo {
        var updState = prevState
        try {
            //Recover info:
            var reqLangCode = updState.reqLangCode
            updState.attachments.usable!!.language = reqLangCode
            updState.playAcknowledge = true
            updState.aiReplies = listOf()   //populate after action

        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return fulfillmentUtils.fallback(updState)   //Error
        }

        Log.d(TAG, updState.toString())
        return updState
    }


    //Process Drive request: PART 1:
    fun driveRequest1(prevState: StateInfo): StateInfo {
        // Manage state:
        var updState = prevState

        //Reply:
        var ttsToRead = ""
        ttsToRead = defaultReplies.replyPlaceRequest(updState.reqLangName)
        val aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = ttsToRead
            )
        )

        //updState:
        updState.interrupt = true
        updState.aiReplies = aiReplies
        Log.d(TAG, updState.toString())

        return updState
    }


    //Process Drive request: PART 2:
    fun driveRequest2(prevState: StateInfo): StateInfo {
        var updState = prevState
        var reqLangCode = updState.reqLangCode

        //PROCESS PLACE INFO:
        var queryText = updState.messages.last().content
        var placeLanguage = reqLangCode   //TODO
        var itemInfo = LibraryItem(
            type = "place"
        )

        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = ExtractorInfo()
        extractorInfo.reqLanguage = reqLangCode

        //Check place in library:
        val libMatch = libUtils.matchLibrary("place", queryText, maxThreshold)
        if (libMatch.matchId < 0) {
            //Place NOT found:
            Log.d(TAG, "DRIVE -> Place from Message")
            itemInfo = nlpExtractor.extractPlace(queryText, reqLangCode)
            itemInfo.url = libUtils.buildPlaceUrlFromItemInfo(itemInfo)
            extractorInfo.matchExtracted = itemInfo.name
            extractorInfo.contextExtracted = itemInfo.detail

        } else {
            //Place found:
            Log.d(TAG, "DRIVE -> Place from Library")
            itemInfo = libUtils.getLibItemById(libMatch.matchId)
            itemInfo.detail = if (itemInfo.address!!.street == "" && itemInfo.address!!.number == "") {
                itemInfo.address!!.town
            } else if (itemInfo.address!!.number == "") {
                itemInfo.address!!.town + ", " + itemInfo.address!!.street
            } else {
                itemInfo.address!!.town + ", " + itemInfo.address!!.street + " " + itemInfo.address!!.number
            }
            extractorInfo.matchExtracted = queryText
            extractorInfo.matchConfirmed = itemInfo.name
            extractorInfo.contextConfirmed = itemInfo.detail
        }

        if (itemInfo.url == "") {
            return fulfillmentUtils.fallback(updState)   //Error

        } else {
            //NAVIGATE:
            //Wait 1 sec:
            Thread.sleep(1000)

            //Reply:
            var introText = defaultReplies.replyPlaceShowIntro()
            var detailText = defaultReplies.replyPlaceShowDetail(itemInfo)
            val aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = introText
                ),
                AiReply(
                    langCode = placeLanguage,
                    text = detailText
                ),
            )

            //updState:
            updState.aiReplies = aiReplies
            updState.actionType = ActionType.OPEN_URL
            updState.playAcknowledge = true
            updState.attachments.nlpExtractor = extractorInfo
            updState.attachments.matchScore = libMatch.matchScore
            updState.attachments.usable = itemInfo
        }

        Log.d(TAG, updState.toString())
        return updState
    }

}