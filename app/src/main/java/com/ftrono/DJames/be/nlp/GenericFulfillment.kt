package com.ftrono.DJames.be.nlp

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.logUtils
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.messLangLower
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.NlpQueryModel
import com.ftrono.DJames.be.models.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo


class GenericFulfillment (private var context: Context) {
    private val TAG = GenericFulfillment::class.java.simpleName


    //Process a request involving a Contact (Call / Message):
    fun contactRequest(resultsNLP: NlpQueryModel): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()
        val filter = "contact"
        var itemInfo = ItemInfoUse(
            type = filter
        )

        //Log:
        logUtils.openLog()
        lastLog.nlpQueries.add(resultsNLP)
        fulfillmentUtils.saveMessage(
            type = "user",
            text = nlp_queryText,
            langCode = resultsNLP.language
        )

        //Extract contact:
        var extractorInfo = ExtractorInfo()
        var nlpExtractor = NLPExtractor(context)
        var contactExtracted = nlpExtractor.extractContact(nlp_queryText)
        extractorInfo.matchExtracted = contactExtracted

        //Match extracted contact name with user library:
        val nlpMatcher = NLPMatcher(context)
        var libMatchId = nlpMatcher.matchLibrary(filter, text=contactExtracted)

        if (libMatchId < 0 || !voiceQueryOn) {
            //Fallback:
            return fulfillmentUtils.fallback(notUnderstood=true)

        } else {
            //Get contact:
            itemInfo = libUtils.getItemInfoUse(filter, libMatchId)
            extractorInfo.matchConfirmed = itemInfo.name
            //Store usable details:
            itemInfo.detail = itemInfo.defaultKey   //TODO: add multiple phones

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

                //dispatcherInfo:
                dispatcherInfo.aiReplies = aiReplies
                dispatcherInfo.actionType = ActionType.CALL
                dispatcherInfo.end = true
                dispatcherInfo.playAcknowledge = true
                dispatcherInfo.usable = itemInfo
                Log.d(TAG, dispatcherInfo.toString())

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
                dispatcherInfo.messageType = nlpExtractor.extractMessageType(nlp_queryText)

                //Reply:
                var ttsToRead = if (dispatcherInfo.messageType == "voice") {
                    defaultReplies.replyMessageRecord(itemInfo.name)
                } else {
                    defaultReplies.replyMessageDictate(itemInfo.name, dispatcherInfo.messageType, reqLangName)
                }
                val aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = ttsToRead
                    )
                )

                //dispatcherInfo:
                dispatcherInfo.aiReplies = aiReplies
                dispatcherInfo.messageMode = true
                dispatcherInfo.reqLanguage = reqLangCode
                dispatcherInfo.usable = itemInfo
                Log.d(TAG, dispatcherInfo.toString())

            } else {
                //Fallback:
                return fulfillmentUtils.fallback(notUnderstood=true)
            }

            //Close log:
            lastLog.nlpExtractor = extractorInfo
            lastLog.usable = itemInfo
            return dispatcherInfo
        }
    }


    //Send a message: PART 2:
    fun sendMessage2(prevDispatch: DispatcherInfo): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()

        // MESSAGE SENDER:
        try {
            //Recover info:
            var reqLangCode = prevDispatch.reqLanguage
            var itemInfo = prevDispatch.usable

            //Store usable details:
            itemInfo.language = reqLangCode

            // Select action to take:
            if (prevDispatch.messageType == "voice") {
                dispatcherInfo.actionType = ActionType.WA_VOICE
            } else if (prevDispatch.messageType == "whatsapp") {
                dispatcherInfo.actionType = ActionType.WA_TEXT
            } else {
                dispatcherInfo.actionType = ActionType.SMS
            }

            //dispatcherInfo:
            dispatcherInfo.usable = itemInfo
            dispatcherInfo.aiReplies = listOf()   //populate after action

        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return fulfillmentUtils.fallback()   //Error
        }

        dispatcherInfo.end = true
        dispatcherInfo.playAcknowledge = true
        Log.d(TAG, dispatcherInfo.toString())

        return dispatcherInfo
    }


    //Process Drive request: PART 1:
    fun driveRequest1(resultsNLP: NlpQueryModel): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()
        logUtils.openLog()
        lastLog.nlpQueries.add(resultsNLP)
        fulfillmentUtils.saveMessage(
            type = "user",
            text = nlp_queryText,
            langCode = resultsNLP.language
        )

        //Detect & process requested languages:
        var detLanguage = resultsNLP.reqLanguage
        var reqLangCode = utils.getLanguageCode(detLanguage, prefs.routeLanguage)
        var reqLangName = utils.getLanguageName(reqLangCode)

        //Reply:
        var ttsToRead = ""
        ttsToRead = defaultReplies.replyRouteRequest(reqLangName)
        val aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = ttsToRead
            )
        )

        //dispatcherInfo:
        dispatcherInfo.aiReplies = aiReplies
        dispatcherInfo.intentName = resultsNLP.intentName
        dispatcherInfo.followUp = true
        dispatcherInfo.reqLanguage = reqLangCode
        Log.d(TAG, dispatcherInfo.toString())

        return dispatcherInfo
    }


    //Process Drive request: PART 2:
    fun driveRequest2(resultsNLP: NlpQueryModel, prevDispatch: DispatcherInfo): DispatcherInfo {
        var dispatcherInfo = DispatcherInfo()
        var reqLangCode = prevDispatch.reqLanguage
        lastLog.nlpQueries.add(resultsNLP)

        //PROCESS ROUTE INFO:
        //item:
        var matchName = nlp_queryText
        var routeLanguage = reqLangCode   //TODO
        var itemInfo = ItemInfoUse(
            type = "route"
        )

        var nlpExtractor = NLPExtractor(context)
        var extractorInfo = ExtractorInfo()
        extractorInfo.reqLanguage = reqLangCode

        //Check route in library:
        val nlpMatcher = NLPMatcher(context)
        val libMatchId = nlpMatcher.matchLibrary("route", matchName, maxThreshold)
        if (libMatchId < 0) {
            //Route NOT found:
            Log.d(TAG, "DRIVE -> Route from Message")
            itemInfo = nlpExtractor.extractRoute(nlp_queryText, reqLangCode)
            itemInfo.url = fulfillmentUtils.buildRouteUrlFromItemInfo(itemInfo)
            extractorInfo.matchExtracted = itemInfo.name
            extractorInfo.contextExtracted = itemInfo.detail

        } else {
            //Route found:
            Log.d(TAG, "DRIVE -> Route from Library")
            itemInfo = libUtils.getItemInfoUse("route", libMatchId)
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
            var introText = defaultReplies.replyRouteShowIntro()
            var detailText = defaultReplies.replyRouteShowDetail(itemInfo)
            val aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = introText
                ),
                AiReply(
                    langCode = routeLanguage,
                    text = detailText
                ),
            )

            //dispatcherInfo:
            dispatcherInfo.aiReplies = aiReplies
            dispatcherInfo.actionType = ActionType.OPEN_URL

            //Player info:
            lastLog.nlpExtractor = extractorInfo
            lastLog.usable = itemInfo
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())
        return dispatcherInfo
    }

}