package com.ftrono.DJames.be.fulfillment

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.messLangLower
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.agents.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.StateInfo


class GenericFulfillment (private var context: Context) {
    private val TAG = this::class.java.simpleName


    //Process a request involving a Contact (Call / Message):
    fun contactRequest(resultsNLP: NlpQueryModel, fromVoice: Boolean = false): StateInfo {
        var stateInfo = StateInfo(
            lastRecording = lastRecordingName
        )
        val filter = "contact"
        var itemInfo = LibraryItem(
            type = filter
        )

        //Extract contact:
        var extractorInfo = ExtractorInfo()
        var nlpExtractor = NLPExtractor(context)
        var contactExtracted = nlpExtractor.extractContact(nlp_queryText)
        extractorInfo.matchExtracted = contactExtracted

        //Match extracted contact name with user library:
        var libMatchId = libUtils.matchLibrary(filter, text=contactExtracted)

        if (libMatchId < 0) {
            //Fallback:
            return fulfillmentUtils.fallback(notUnderstood=true)

        } else {
            //Get contact:
            itemInfo = libUtils.getLibItemById(libMatchId)
            extractorInfo.matchConfirmed = itemInfo.name

            //CASES:
            if (resultsNLP.intentName.contains("Call")) {
                //A) CALL:
                //Reply:
                val ttsToRead = defaultReplies.replyCalling(itemInfo.name)
                val aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = ttsToRead
                    )
                )

                //stateInfo:
                stateInfo.aiReplies = aiReplies
                stateInfo.actionType = ActionType.CALL
                stateInfo.end = true
                stateInfo.playAcknowledge = true
                stateInfo.usable = itemInfo
                Log.d(TAG, stateInfo.toString())

            } else if (resultsNLP.intentName.contains("Message")) {
                //B) MESSAGE:
                //Check if voice request contains a specific requested language:
                var reqLangName = resultsNLP.reqLanguage
                var reqLangCode = utils.getLanguageCode(reqLangName)

                //If no specific language requested -> use contact preferences or global preferences:
                if (reqLangCode == "") {
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
                stateInfo.messageType = nlpExtractor.extractMessageType(nlp_queryText)
                // Select action to take:
                if (stateInfo.messageType == "voice") {
                    stateInfo.actionType = ActionType.WA_VOICE
                    if (!fromVoice) {
                        //Fallback:
                        return fulfillmentUtils.fallback(cannotRecordWAVoice=true)
                    }
                } else if (stateInfo.messageType == "whatsapp") {
                    stateInfo.actionType = ActionType.WA_TEXT
                } else {
                    stateInfo.actionType = ActionType.SMS
                }

                //Reply:
                var ttsToRead = if (stateInfo.messageType == "voice") {
                    defaultReplies.replyMessageRecord(itemInfo.name)
                } else {
                    defaultReplies.replyMessageDictate(itemInfo.name, stateInfo.messageType, reqLangName)
                }
                val aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = ttsToRead
                    )
                )

                //stateInfo:
                stateInfo.aiReplies = aiReplies
                stateInfo.messageMode = true
                stateInfo.reqLanguage = reqLangCode
                stateInfo.usable = itemInfo
                Log.d(TAG, stateInfo.toString())

            } else {
                //Fallback:
                return fulfillmentUtils.fallback(notUnderstood=true)
            }

            //Update message:
            lastAiMessage.actionType = stateInfo.actionType
            lastAiMessage.attachments.nlpExtractor = extractorInfo
            lastAiMessage.attachments.usable = itemInfo
            return stateInfo
        }
    }


    //Send a message: PART 2:
    fun sendMessage2(prevState: StateInfo): StateInfo {
        var stateInfo = StateInfo(
            lastRecording = lastRecordingName
        )

        // MESSAGE SENDER:
        try {
            //Recover info:
            var reqLangCode = prevState.reqLanguage
            var itemInfo = prevState.usable
            stateInfo.actionType = prevState.actionType

            //Store usable details:
            itemInfo.language = reqLangCode

            //stateInfo:
            stateInfo.usable = itemInfo
            stateInfo.aiReplies = listOf()   //populate after action

        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return fulfillmentUtils.fallback()   //Error
        }

        stateInfo.end = true
        stateInfo.playAcknowledge = true
        Log.d(TAG, stateInfo.toString())

        return stateInfo
    }


    //Process Drive request: PART 1:
    fun driveRequest1(resultsNLP: NlpQueryModel): StateInfo {
        var stateInfo = StateInfo(
            lastRecording = lastRecordingName
        )

        //Detect & process requested languages:
        var detLanguage = resultsNLP.reqLanguage
        var reqLangCode = utils.getLanguageCode(detLanguage, prefs.placeLanguage)
        var reqLangName = utils.getLanguageName(reqLangCode)

        //Reply:
        var ttsToRead = ""
        ttsToRead = defaultReplies.replyPlaceRequest(reqLangName)
        val aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = ttsToRead
            )
        )

        //stateInfo:
        stateInfo.aiReplies = aiReplies
        stateInfo.intentName = resultsNLP.intentName
        stateInfo.reqLanguage = reqLangCode
        Log.d(TAG, stateInfo.toString())

        return stateInfo
    }


    //Process Drive request: PART 2:
    fun driveRequest2(resultsNLP: NlpQueryModel, prevState: StateInfo): StateInfo {
        var stateInfo = StateInfo(
            lastRecording = lastRecordingName
        )
        var reqLangCode = prevState.reqLanguage

        //PROCESS PLACE INFO:
        //item:
        var matchName = nlp_queryText
        var placeLanguage = reqLangCode   //TODO
        var itemInfo = LibraryItem(
            type = "place"
        )

        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = ExtractorInfo()
        extractorInfo.reqLanguage = reqLangCode

        //Check place in library:
        val libMatchId = libUtils.matchLibrary("place", matchName, maxThreshold)
        if (libMatchId < 0) {
            //Place NOT found:
            Log.d(TAG, "DRIVE -> Place from Message")
            itemInfo = nlpExtractor.extractPlace(nlp_queryText, reqLangCode)
            itemInfo.url = libUtils.buildPlaceUrlFromItemInfo(itemInfo)
            extractorInfo.matchExtracted = itemInfo.name
            extractorInfo.contextExtracted = itemInfo.detail

        } else {
            //Place found:
            Log.d(TAG, "DRIVE -> Place from Library")
            itemInfo = libUtils.getLibItemById(libMatchId)
            itemInfo.detail = itemInfo.address!!.town + ", " + itemInfo.address!!.street + itemInfo.address!!.number
            extractorInfo.matchExtracted = nlp_queryText
            extractorInfo.matchConfirmed = itemInfo.name
            extractorInfo.contextConfirmed = itemInfo.detail
        }

        if (itemInfo.url == "") {
            return fulfillmentUtils.fallback()   //Error

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

            //stateInfo:
            stateInfo.aiReplies = aiReplies
            stateInfo.actionType = ActionType.OPEN_URL
            stateInfo.usable = itemInfo
            stateInfo.playAcknowledge = true

            //Update message:
            lastAiMessage.actionType = stateInfo.actionType
            lastAiMessage.attachments.nlpExtractor = extractorInfo
            lastAiMessage.attachments.usable = itemInfo
        }

        //Build return
        stateInfo.end = true
        Log.d(TAG, stateInfo.toString())
        return stateInfo
    }

}