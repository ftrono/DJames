package com.ftrono.DJames.be.nlp

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxThreshold
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.messLangLower
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.be.database.ItemInfoUse
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.serialization.json.Json


class GenericFulfillment (private var context: Context) {
    private val TAG = GenericFulfillment::class.java.simpleName


    //Make a call:
    fun makeCall(resultsNLP: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        utils.openLog()

        //Log:
        last_log!!.addProperty("intent_name", resultsNLP.get("intent_name").asString)
        last_log!!.add("nlp", resultsNLP)

        //Extract contact:
        var nlpExtractor = NLPExtractor(context)
        var contact_extractor = nlpExtractor.extractContact(nlp_queryText)
        var contact_name = contact_extractor.get("contact_confirmed").asString
        var phone = contact_extractor.get("contact_phone").asString

        if (phone == "") {
            //Fallback:
            return utils.fallback("Sorry, I did not understand!")

        } else {
            //Prepare toast text:
            var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }
            if (contact_name != "") {
                toastText = toastText.replace(contact_extractor.get("contact_extracted").asString, contact_name.uppercase())
            }

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", toastText)
                context.sendBroadcast(intent)
            }

            //Read:
            var ttsToRead = "Calling ${contact_name}..."
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)
        }

        fulfillmentUtils.releaseAudioFocus()

        //processStatus:
        processStatus.addProperty("stopService", true)
        processStatus.addProperty("stopSound", true)
        Log.d(TAG, processStatus.toString())

        //Close log:
        utils.closeLog(context)

        //CALL:
        Intent().also { intent ->
            intent.setAction(ACTION_MAKE_CALL)
            intent.putExtra("toCall", "tel:${phone}")
            context.sendBroadcast(intent)
        }
        return processStatus
    }


    //Send a message: PART 1:
    fun sendMessage1(resultsNLP: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        utils.openLog()

        //Log:
        last_log!!.addProperty("intent_name", resultsNLP.get("intent_name").asString)
        last_log!!.add("nlp", resultsNLP)

        //Check if voice request contains a specific requested language:
        var reqLangName = ""
        var reqLangCode = ""
        try {
            //Get requested messaging language:
            val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.language_codes)))
            val sourceJson = JsonParser.parseReader(reader).asJsonObject
            reqLangName = resultsNLP.get("reqLanguage").asString
            reqLangCode = sourceJson[reqLangName].asString
            Log.d(TAG, "REQUESTED MESSAGING LANGUAGE: $reqLangCode")
        } catch (e: Exception) {
            Log.w(TAG, "No specific language provided in the voice request.", e)
        }

        //Extract contact:
        var nlpExtractor = NLPExtractor(context)
        var contact_extractor = nlpExtractor.extractContact(nlp_queryText, fullLanguage=reqLangName)
        var contact_name = contact_extractor.get("contact_confirmed").asString
        var phone = contact_extractor.get("contact_phone").asString

        //If no specific language requested -> use contact preferences or global preferences:
        if (reqLangCode == "") {
            try {
                //Contact preferences:
                reqLangCode = contact_extractor.get("contact_language").asString
                reqLangName = messLangLower[messLangCodes.indexOf(reqLangCode)]
                Log.d(TAG, "PREFERRED CONTACT LANGUAGE: $reqLangCode")
            } catch (e: Exception) {
                //Global preferences:
                reqLangCode = prefs.messageLanguage
                reqLangName = messLangFull[messLangCodes.indexOf(prefs.messageLanguage)]
                Log.d(TAG, "Messaging in default language: $reqLangCode")
            }
        }

        if (phone == "" || !voiceQueryOn) {
            //Fallback:
            return utils.fallback("Sorry, I did not understand!")

        } else {
            //Prepare toast text:
            var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }
            if (contact_name != "") {
                toastText = toastText.replace(contact_extractor.get("contact_extracted").asString, contact_name.uppercase())
            }

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", toastText)
                context.sendBroadcast(intent)
            }

            //Read:
            var ttsToRead = "Please, dictate the message for ${contact_name} in $reqLangName."
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)
        }

        //processStatus:
        processStatus.addProperty("messageMode", true)
        processStatus.addProperty("reqLanguage", reqLangCode)
        processStatus.add("contact_extractor", contact_extractor)
        Log.d(TAG, processStatus.toString())

        //Close log:
        utils.closeLog(context)
        return processStatus
    }


    //Send a message: PART 2:
    fun sendMessage2(prevStatus: JsonObject) : JsonObject {
        var processStatus = JsonObject()

        // MESSAGE SENDER:
        try {
            //Recover info:
            var reqLangCode = prevStatus.get("reqLanguage").asString
            var contact_extractor = prevStatus.get("contact_extractor").asJsonObject
            var contact_name = contact_extractor.get("contact_confirmed").asString
            var phone = contact_extractor.get("contact_phone").asString

            //Send SMS:
            val smsManager: SmsManager = SmsManager.getDefault()
            var messageText = fulfillmentUtils.replaceEmojis(context=context, text=nlp_queryText, reqLanguage=reqLangCode)

            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            //smsManager.sendTextMessage(phone, null, messageText, null, null)

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", "SMS sent to ${contact_name.uppercase()}")
                context.sendBroadcast(intent)
            }

            //Read:
            var ttsToRead = "Message sent to ${contact_name}!"
            val itemsToRead = listOf(
                mapOf(
                    "language" to prefs.queryLanguage,
                    "text" to ttsToRead
                )
            )
            fulfillmentUtils.ttsRead(context, itemsToRead, dimAudio=false)


        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return utils.fallback("ERROR: SMS not sent!")
        }

        processStatus.addProperty("stopService", true)
        processStatus.addProperty("stopSound", true)
        Log.d(TAG, processStatus.toString())
        fulfillmentUtils.releaseAudioFocus()

        return processStatus
    }


    //Process Drive request: PART 1:
    fun driveRequest1(resultsNLP: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        utils.openLog()

        //Detect & process requested languages:
        var intentName = resultsNLP.get("intent_name").asString
        var detLanguage = resultsNLP.get("reqLanguage").asString
        var reqLangCode = utils.getLanguageCode(context, detLanguage, prefs.routeLanguage)
        var reqLangName = utils.getLanguageName(context, reqLangCode)

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

        //processStatus:
        processStatus.addProperty("followUp", true)
        processStatus.addProperty("reqLanguage", reqLangCode)
        processStatus.addProperty("intent_name", intentName)
        Log.d(TAG, processStatus.toString())

        //Log:
        last_log!!.addProperty("intent_name", intentName)
        return processStatus
    }


    //Process Drive request: PART 2:
    fun driveRequest2(resultsNLP: JsonObject, prevStatus: JsonObject) : JsonObject {
        var processStatus = JsonObject()
        var reqLangCode = prevStatus.get("reqLanguage").asString
        last_log!!.add("nlp", resultsNLP)

        //Prepare toast text:
        var toastText = nlp_queryText.replaceFirstChar { it.uppercase() }

        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }


        //PROCESS PLAY INFO:
        //item:
        var matchName = nlp_queryText
        var routeMatchId = ""
        var routeConfirmed = ""
        var detailConfirmed = ""
        var routeUrl = ""
        var routeLanguage = reqLangCode
        var routeInfo = JsonObject()

        var extractorInfo = JsonObject()
        extractorInfo.addProperty("match_extracted", nlp_queryText)
        extractorInfo.addProperty("text_confirmed", nlp_queryText)

        var nlpExtractor = NLPExtractor(context)
        //Check route in vocabulary:
        routeMatchId = nlpExtractor.matchVocabulary("route", matchName, maxThreshold)
        if (routeMatchId == "") {
            //Route not found:
            Log.d(TAG, "DRIVE -> Route from Message")
            last_log!!.addProperty("voc_score", 100)
            routeInfo = fulfillmentUtils.buildRouteUrlFromMessage(nlp_queryText, reqLangCode)
            routeConfirmed = routeInfo.get("name").asString
            detailConfirmed = routeInfo.get("detail").asString
            routeLanguage = routeInfo.get("language").asString
            routeUrl = routeInfo.get("url").asString

        } else {
            //Route found:
            Log.d(TAG, "DRIVE -> Route from Library")
            val itemInfo = libUtils.getItemInfoUse("route", routeMatchId)
            routeConfirmed = itemInfo.name
            detailConfirmed = itemInfo.detail
            routeLanguage = itemInfo.language   //TODO
            routeUrl = itemInfo.url
            extractorInfo.addProperty("text_confirmed", routeConfirmed)
            extractorInfo.addProperty("detail_confirmed", detailConfirmed)
            //Route info:
            routeInfo.addProperty("name", routeConfirmed)
            routeInfo.addProperty("detail", detailConfirmed)
            routeInfo.addProperty("language", itemInfo.language)   //TODO
            routeInfo.addProperty("url", routeUrl)
        }

        if (routeUrl == "") {
            //Close log:
            //utils.closeLog(context)
            return utils.fallback()
        } else {
            //NAVIGATE:
            fulfillmentUtils.releaseAudioFocus()

            //Wait 1 sec:
            Thread.sleep(1000)

            //Read TTS:
            var ttsToRead = ""
            if (detailConfirmed == "") {
                ttsToRead = "${routeConfirmed}!"
            } else {
                ttsToRead = "${routeConfirmed}, ${detailConfirmed}!"
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
            last_log!!.add("nlp_extractor", extractorInfo)
            last_log!!.add("route_info", routeInfo)

            //DRIVE TO ROUTE:
            utils.openLink(context, url = routeUrl, fromService = true)
        }

        //Build return
        processStatus.addProperty("stopService", true)
        Log.d(TAG, processStatus.toString())

        //Close log:
        utils.closeLog(context)
        return processStatus
    }

}