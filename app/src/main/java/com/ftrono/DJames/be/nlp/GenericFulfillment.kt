package com.ftrono.DJames.be.nlp

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.ACTION_TOASTER
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
import com.ftrono.DJames.be.models.DispatcherInfo
import com.google.gson.JsonObject


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
            return fulfillmentUtils.fallback("Sorry, I did not understand!")

        } else {
            //Get contact:
            itemInfo = libUtils.getItemInfoUse(filter, libMatchId)

            //Log:
            extractorInfo.matchConfirmed = itemInfo.name
            lastLog.nlpExtractor = extractorInfo
            lastLog.usable = itemInfo

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
                var ttsToRead = "Calling ${itemInfo.name}..."
                val itemsToRead = listOf(
                    mapOf(
                        "language" to prefs.queryLanguage,
                        "text" to ttsToRead
                    )
                )
                fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio = false)
                fulfillmentUtils.releaseAudioFocus()

                //dispatcherInfo:
                dispatcherInfo.end = true
                dispatcherInfo.playAcknowledge = true
                Log.d(TAG, dispatcherInfo.toString())

                //CALL:
                Intent().also { intent ->
                    intent.setAction(ACTION_MAKE_CALL)
                    intent.putExtra("toCall", "tel:${contactPhone}")
                    context.sendBroadcast(intent)
                }

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

                //Read:
                var ttsToRead = "Please, dictate the message for ${itemInfo.name} in $reqLangName."
                val itemsToRead = listOf(
                    mapOf(
                        "language" to prefs.queryLanguage,
                        "text" to ttsToRead
                    )
                )
                fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)

                //dispatcherInfo:
                dispatcherInfo.usable = itemInfo
                dispatcherInfo.messageMode = true
                dispatcherInfo.reqLanguage = reqLangCode
                Log.d(TAG, dispatcherInfo.toString())

            } else {
                //Fallback:
                return fulfillmentUtils.fallback("Sorry, I did not understand!")
            }

            //Close log:
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

            //Send SMS:
            val smsManager: SmsManager = SmsManager.getDefault()
            var messageText = fulfillmentUtils.replaceEmojis(context=context, text=nlp_queryText, reqLanguage=reqLangCode)

            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(contactPhone, null, parts, null, null)
            //smsManager.sendTextMessage(phone, null, messageText, null, null)

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", "SMS sent to ${itemInfo.name.uppercase()}")
                context.sendBroadcast(intent)
            }

            //Read:
            var ttsToRead = "Message sent to ${itemInfo.name}!"
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)


        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return fulfillmentUtils.fallback("ERROR: SMS not sent!")
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
        ttsToRead = "Tell me the route you need in ${reqLangName}."
        val itemsToRead = listOf(
            mapOf(
                "language" to prefs.queryLanguage,
                "text" to ttsToRead
            )
        )
        fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)

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
            var ttsToRead = ""
            if (itemInfo.detail == "") {
                ttsToRead = "${itemInfo.name}!"
            } else {
                ttsToRead = "${itemInfo.name}, ${itemInfo.detail}!"
            }
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to "Here's the route to: "
                ),
                mapOf(
                    "language" to routeLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=true)

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