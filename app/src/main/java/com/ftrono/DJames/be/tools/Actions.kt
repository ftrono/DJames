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
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recFileName
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.models.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.spotify.SpotifyPlayer
import java.io.File


class Actions(
    private val context: Context
) {
    private val TAG = Actions::class.java.simpleName
    private var dispatcherInfo = DispatcherInfo()

    // MAIN: EXECUTOR:
    fun execute(
        latestDispatch: DispatcherInfo
    ): List<AiReply> {
        dispatcherInfo = latestDispatch
        var updatedReplies = listOf<AiReply>()
        // Execute action:
        var stringReply = when (dispatcherInfo.actionType) {
            ActionType.PLAY -> spotifyPlay()
            ActionType.CALL -> makeCall()
            ActionType.SMS -> sendSMS()
            ActionType.WA_TEXT -> sendWhatsappText()
            ActionType.WA_VOICE -> sendWhatsappAudio()
            ActionType.OPEN_URL -> openLink()
            else -> ""
        }
        // Update replies:
        if (stringReply != "") {
            updatedReplies = listOf<AiReply>(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = stringReply
                )
            )
        }
        return updatedReplies
    }

    // SPOTIFY PLAY:
    fun spotifyPlay(): String {
        val spotifyPlayer = SpotifyPlayer(context)
        spotifyPlayer.spotifyPlay(dispatcherInfo.playable)
        return ""
    }

    //CALL:
    fun makeCall(): String {
        try {
            val defaultPhoneSet = dispatcherInfo.usable.phoneSets[dispatcherInfo.usable.detail]!!
            val contactPhone = "${defaultPhoneSet.prefix}${defaultPhoneSet.phone}"
            Intent().also { intent ->
                intent.setAction(ACTION_MAKE_CALL)
                intent.putExtra("toCall", "tel:$contactPhone")
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "makeCall(): TOOL ERROR: ", e)
        }
        return ""
    }


    //SEND SMS:
    fun sendSMS(): String {
        try {
            val contactName = dispatcherInfo.usable.name
            val defaultPhoneSet = dispatcherInfo.usable.phoneSets[dispatcherInfo.usable.detail]!!
            val contactPhone = "${defaultPhoneSet.prefix}${defaultPhoneSet.phone}"
            var messageText = "[DJ] ${
                fulfillmentUtils.replaceEmojis(
                    context = context,
                    text = nlp_queryText,
                    reqLanguage = dispatcherInfo.reqLanguage
                )
            }"
            val smsManager: SmsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(contactPhone, null, parts, null, null)
            val ttsToRead = defaultReplies.replySmsSent(contactName)
            Log.d(TAG, ttsToRead)
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendSMS(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }


    //SEND WHATSAPP TEXT:
    fun sendWhatsappText(): String {
        try {
            val contactName = dispatcherInfo.usable.name
            val defaultPhoneSet = dispatcherInfo.usable.phoneSets[dispatcherInfo.usable.detail]!!
            val contactPhone = "${defaultPhoneSet.prefix}${defaultPhoneSet.phone}"
            var messageText = "[DJ] ${
                fulfillmentUtils.replaceEmojis(
                    context = context,
                    text = nlp_queryText,
                    reqLanguage = dispatcherInfo.reqLanguage
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
            Log.d(TAG, "Whatsapp text message ready to be sent!")
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendWhatsappText(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }


    //SEND WHATSAPP AUDIO:
    fun sendWhatsappAudio(): String {
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
            val contactName = dispatcherInfo.usable.name
            context.startActivity(intent)
            val ttsToRead = defaultReplies.replyWAVoiceSent(contactName)
            Log.d(TAG, "Whatsapp audio message ready to be sent!")
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendWhatsappAudio(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }


    // OPEN LINK:
    fun openLink(): String {
        val urlToOpen = dispatcherInfo.usable.url
        utils.openLink(context, url = urlToOpen, fromService = true)
        return ""
    }

}