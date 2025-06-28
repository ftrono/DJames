package com.ftrono.DJames.be.tools

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import android.net.Uri
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.application.recFileName
import java.io.File


fun sendSMS(context: Context, contactPhone: String, contactName: String, reqLangCode: String): String {
    //SEND SMS:
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
    val ttsToRead = "SMS sent to $contactName!"
    Log.d("Tools", ttsToRead)
    return ttsToRead
}


fun sendWhatsappText(context: Context, contactPhone: String, contactName: String, reqLangCode: String): String {
    //SEND WHATSAPP TEXT:
    var messageText = "[DJ] ${
        fulfillmentUtils.replaceEmojis(
            context = context,
            text = nlp_queryText,
            reqLanguage = reqLangCode
        )
    }"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/${contactPhone.replace("+", "")}?text=${Uri.encode(messageText)}")
        setPackage("com.whatsapp")
        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("fromwhere", "ser")
    }
    //Try to send:
    context.startActivity(intent)
    val ttsToRead = "Message for $contactName ready: please, click on SEND in Whatsapp!"
    Log.d("Tools", "Whatsapp text message ready to be sent!")
    return ttsToRead
}


fun sendWhatsappAudio(context: Context, contactName: String): String {
    //SEND WHATSAPP AUDIO:
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
    val ttsToRead = "Voice message for $contactName ready: please, select the contact in Whatsapp and SEND it!"
    Log.d("Tools", "Whatsapp audio message ready to be sent!")
    return ttsToRead
}