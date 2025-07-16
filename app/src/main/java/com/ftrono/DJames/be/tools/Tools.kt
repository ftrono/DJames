package com.ftrono.DJames.be.tools

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import android.net.Uri
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.recFileName
import java.io.File


class Tools() {

    private val TAG = Tools::class.java.simpleName

    //CALL:
    fun makeCall(
        context: Context,
        contactPhone: String
    ) {
        try {
            Intent().also { intent ->
                intent.setAction(ACTION_MAKE_CALL)
                intent.putExtra("toCall", "tel:${contactPhone}")
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.d(TAG, "makeCall(): TOOL ERROR: ", e)
        }
    }


    //SEND SMS:
    fun sendSMS(
        context: Context,
        contactPhone: String,
        contactName: String,
        reqLangCode: String
    ): String {
        try {
            var messageText = "[DJ] ${
                fulfillmentUtils.replaceEmojis(
                    context = context,
                    text = nlp_queryText,
                    reqLanguage = reqLangCode
                )
            }"
            val smsManager: SmsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(contactPhone, null, parts, null, null)
            val ttsToRead = defaultReplies.replySmsSent(contactName)
            Log.d("Tools", ttsToRead)
            return ttsToRead

        } catch (e: Exception) {
            Log.d(TAG, "sendSMS(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }


    //SEND WHATSAPP TEXT:
    fun sendWhatsappText(
        context: Context,
        contactPhone: String,
        contactName: String,
        reqLangCode: String
    ): String {
        try {
            var messageText = "[DJ] ${
                fulfillmentUtils.replaceEmojis(
                    context = context,
                    text = nlp_queryText,
                    reqLanguage = reqLangCode
                )
            }"

            //Open WA:
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://wa.me/${contactPhone.replace("+", "")}?text=${
                        Uri.encode(messageText)
                    }"
                )
                setPackage("com.whatsapp")
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("fromwhere", "ser")
            }

            //Try to send:
            context.startActivity(intent)
            val ttsToRead = defaultReplies.replyWATextSent(contactName)
            Log.d("Tools", "Whatsapp text message ready to be sent!")
            return ttsToRead

        } catch (e: Exception) {
            Log.d(TAG, "sendWhatsappText(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }


    //SEND WHATSAPP AUDIO:
    fun sendWhatsappAudio(context: Context, contactName: String): String {
        try {
            //Get audio file:
            val audioFile = File(context.cacheDir, "$recFileName.mp3")
            val uri = FileProvider.getUriForFile(context, "com.ftrono.DJames.provider", audioFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.whatsapp")
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("fromwhere", "ser")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            //Try to send:
            context.startActivity(intent)
            val ttsToRead = defaultReplies.replyWAVoiceSent(contactName)
            Log.d("Tools", "Whatsapp audio message ready to be sent!")
            return ttsToRead

        } catch (e: Exception) {
            Log.d(TAG, "sendWhatsappAudio(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

}