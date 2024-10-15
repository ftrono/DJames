package com.ftrono.DJames.nlp

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.messLangCaps
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.messLangNames
import com.ftrono.DJames.application.voiceQueryOn
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader


class GenericFulfillment (private var context: Context) {
    private val TAG = GenericFulfillment::class.java.simpleName
    private val utils = Utilities()


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

            //TODO: eng only!
            var ttsToRead = "Calling ${contact_name}..."
            utils.ttsRead(context, prefs.queryLanguage, ttsToRead, dimAudio=false)
        }

        utils.releaseAudioFocus()

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
                reqLangName = contact_extractor.get("contact_language").asString
                reqLangCode = messLangCodes[messLangNames.indexOf(reqLangName)]
                Log.d(TAG, "PREFERRED CONTACT LANGUAGE: $reqLangCode")
            } catch (e: Exception) {
                //Global preferences:
                reqLangName = messLangCaps[messLangCodes.indexOf(prefs.messageLanguage)]
                reqLangCode = prefs.messageLanguage
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

            //TODO: eng only!
            var ttsToRead = "Please, dictate the message for ${contact_name} in $reqLangName."
            utils.ttsRead(context, prefs.queryLanguage, ttsToRead, dimAudio=false)
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
            var messageText = utils.replaceEmojis(context=context, text=nlp_queryText, reqLanguage=reqLangCode)

            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            //smsManager.sendTextMessage(phone, null, messageText, null, null)

            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", "SMS sent to ${contact_name.uppercase()}")
                context.sendBroadcast(intent)
            }

            //TODO: eng only!
            var ttsToRead = "Message sent to ${contact_name}!"
            utils.ttsRead(context, prefs.queryLanguage, ttsToRead, dimAudio=false)


        } catch (e: Exception) {
            Log.w(TAG, "sendMessage2: EXCEPTION: ", e)
            return utils.fallback("ERROR: SMS not sent!")
        }

        processStatus.addProperty("stopService", true)
        processStatus.addProperty("stopSound", true)
        Log.d(TAG, processStatus.toString())
        utils.releaseAudioFocus()

        return processStatus
    }

}