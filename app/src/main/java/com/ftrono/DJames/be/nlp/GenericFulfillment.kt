package com.ftrono.DJames.be.nlp

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.ACTION_TOASTER
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
import com.ftrono.DJames.application.tools
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.NlpQueryModel
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
            return fulfillmentUtils.fallback()

        } else {
            //Get contact:
            itemInfo = libUtils.getItemInfoUse(filter, libMatchId)
            extractorInfo.matchConfirmed = itemInfo.name

            //Prepare toast text with actual contact name:
            var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }
            toastText = toastText.replace(
                contactExtracted,
                itemInfo.name.uppercase()
            )

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", toastText)
                context.sendBroadcast(intent)
            }

            //CASES:
            if (resultsNLP.intentName.contains("Call")) {
                //A) CALL:
                val defaultPhoneSet = itemInfo.phoneSets[itemInfo.defaultKey]!!   //TODO: add multiple phones
                val contactPhone = "${defaultPhoneSet.prefix}${defaultPhoneSet.phone}"

                //Read:
                val ttsToRead = defaultReplies.replyCalling(itemInfo.name)
                val itemsToRead = listOf(
                    mapOf(
                        "language" to prefs.queryLanguage,
                        "text" to ttsToRead
                    )
                )
                fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio = false)
                fulfillmentUtils.releaseAudioFocus()
                fulfillmentUtils.saveMessage(
                    type = "ai",
                    text = ttsToRead
                )

                //dispatcherInfo:
                dispatcherInfo.end = true
                dispatcherInfo.playAcknowledge = true
                Log.d(TAG, dispatcherInfo.toString())

                //CALL:
                tools.makeCall(context, contactPhone)

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

                //Read:
                var ttsToRead = if (dispatcherInfo.messageType == "voice") {
                    defaultReplies.replyMessageRecord(itemInfo.name)
                } else {
                    defaultReplies.replyMessageDictate(itemInfo.name, dispatcherInfo.messageType, reqLangName)
                }
                val itemsToRead = listOf(
                    mapOf(
                        "language" to prefs.queryLanguage,
                        "text" to ttsToRead
                    )
                )
                fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)
                fulfillmentUtils.saveMessage(
                    type = "ai",
                    text = ttsToRead
                )

                //dispatcherInfo:
                dispatcherInfo.usable = itemInfo
                dispatcherInfo.messageMode = true
                dispatcherInfo.reqLanguage = reqLangCode
                Log.d(TAG, dispatcherInfo.toString())

            } else {
                //Fallback:
                return fulfillmentUtils.fallback()
            }

            //Close log:
            lastLog.nlpExtractor = extractorInfo
            lastLog.usable = itemInfo
            logUtils.storeLog(context)

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
            val defaultPhoneSet = itemInfo.phoneSets[itemInfo.defaultKey]!!   //TODO: add multiple phones
            val contactPhone = "${defaultPhoneSet.prefix}${defaultPhoneSet.phone}"
            var ttsToRead = ""
            var toastText = ""

            if (prevDispatch.messageType == "voice") {
                //SEND WHATSAPP AUDIO:
                ttsToRead = tools.sendWhatsappAudio(context, itemInfo.name)
                toastText = "Voice message ready: use Whatsapp to SEND!"
            } else if (prevDispatch.messageType == "whatsapp") {
                //SEND WHATSAPP TEXT:
                ttsToRead = tools.sendWhatsappText(context, contactPhone, itemInfo.name, reqLangCode)
                toastText = "Text message ready: use Whatsapp to SEND!"
            } else {
                //SEND SMS:
                ttsToRead = tools.sendSMS(context, contactPhone, itemInfo.name, reqLangCode)
                toastText = "SMS sent to ${itemInfo.name.uppercase()}!"
            }

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", ttsToRead)
                context.sendBroadcast(intent)
            }

            //Read:
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead.lowercase()
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)
            fulfillmentUtils.saveMessage(
                type = "ai",
                text = ttsToRead
            )

        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return fulfillmentUtils.fallback("ERROR: Message not sent!")
        }

        dispatcherInfo.end = true
        dispatcherInfo.playAcknowledge = true
        Log.d(TAG, dispatcherInfo.toString())
        fulfillmentUtils.releaseAudioFocus()

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

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }

        //Distinguish by intent & build voice response:
        var ttsToRead = ""

        //Read:
        ttsToRead = defaultReplies.replyRouteRequest(reqLangName)
        val itemsToRead = listOf(
            mapOf(
                "language" to prefs.queryLanguage,
                "text" to ttsToRead
            )
        )
        fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)
        fulfillmentUtils.saveMessage(
            type = "ai",
            text = ttsToRead
        )

        //dispatcherInfo:
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

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }


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
            //Close log:
            //logUtils.storeLog(context)
            return fulfillmentUtils.fallback()

        } else {
            //NAVIGATE:
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //Read TTS:
            var introText = defaultReplies.replyRouteShowIntro()
            var detailText = defaultReplies.replyRouteShowDetail(itemInfo)
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to introText
                ),
                mapOf(
                    "language" to routeLanguage,
                    "text" to detailText
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=true)
            fulfillmentUtils.saveMessage(
                type = "ai",
                text = introText + detailText
            )

            //Player info:
            lastLog.nlpExtractor = extractorInfo
            lastLog.usable = itemInfo

            //DRIVE TO ROUTE:
            utils.openLink(context, url = itemInfo.url, fromService = true)
        }

        //Build return
        dispatcherInfo.end = true
        Log.d(TAG, dispatcherInfo.toString())

        //Close log:
        logUtils.storeLog(context)
        return dispatcherInfo
    }

}