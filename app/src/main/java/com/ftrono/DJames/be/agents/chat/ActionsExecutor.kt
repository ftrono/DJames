package com.ftrono.DJames.be.agents.chat

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import android.net.Uri
import com.ftrono.DJames.BuildConfig
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recDir
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.agents.data.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.spotify.SpotifyPlayer
import java.io.File


class ActionsExecutor(
    private val context: Context
) {
    private val TAG = this::class.java.simpleName

    // HELPER: LAUNCHER ONLY:
    fun launchAction(
        action: ActionType,
        text: String,
        usable: LibraryItem? = null,
        playable: SpotifyPlayable? = null,
        reqLanguage: String = "",
        fromOldChat: Boolean = false,
        lastRecording: String = "",
    ): String {
        return when (action) {
            ActionType.PLAY -> spotifyPlay(playable)
            ActionType.CALL -> makeCall(usable, fromOldChat)
            ActionType.SMS -> sendSMS(text, usable, reqLanguage)
            ActionType.WA_TEXT -> sendWhatsappText(text, usable, reqLanguage)
            ActionType.WA_VOICE -> sendWhatsappAudio(usable, lastRecording)
            ActionType.OPEN_URL -> if (prefs.enableV3) "" else openLink(usable)
        }
    }

    // MAIN: EXECUTOR:
    fun execute(
        latestState: StateInfo
    ): List<AiReply> {
        var updatedReplies = listOf<AiReply>()
        // Execute action:
        var stringReply = if (latestState.actionType == null) "" else {
            launchAction(
                action = latestState.actionType!!,
                text = latestState.messages.last().content,
                usable = latestState.attachments.usable,
                playable = latestState.attachments.spotifyPlay,
                reqLanguage = latestState.reqLangCode,
                lastRecording = latestState.lastRecording
            )
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
    fun spotifyPlay(
        playable: SpotifyPlayable?
    ): String {
        try {
            val spotifyPlayer = SpotifyPlayer(context)
            spotifyPlayer.spotifyPlay(playable!!)
        } catch (e: Exception) {
            Log.w(TAG, "spotifyPlay(): ACTION ERROR: ", e)
        }
        return ""
    }

    //CALL:
    fun makeCall(
        usable: LibraryItem?,
        fromOldChat: Boolean = false,
    ): String {
        try {
            val phoneSet = usable!!.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            utils.makeCall(context, contactPhone, fromService = !fromOldChat)
        } catch (e: Exception) {
            Log.w(TAG, "makeCall(): ACTION ERROR: ", e)
        }
        return ""
    }

    //SEND SMS:
    fun sendSMS(
        text: String,
        usable: LibraryItem?,
        reqLanguage: String
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable!!.name
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            // SEND:
            var messageText = "[DJ] ${
                fulfillmentUtils.replaceEmojis(
                    context = context,
                    text = text,
                    reqLanguage = reqLanguage
                )
            }"
            val smsManager: SmsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(contactPhone, null, parts, null, null)
            ttsToRead = defaultReplies.replySmsSent(contactName)
            Log.d(TAG, ttsToRead)
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendSMS(): ACTION ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    //SEND WHATSAPP TEXT:
    fun sendWhatsappText(
        text: String,
        usable: LibraryItem?,
        reqLanguage: String,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable!!.name
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            // SEND:
            var messageText = "[DJ] ${
                fulfillmentUtils.replaceEmojis(
                    context = context,
                    text = text,
                    reqLanguage = reqLanguage
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
            ttsToRead = defaultReplies.replyWATextSent(contactName)
            Log.d(TAG, "Whatsapp text message ready to be sent!")
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendWhatsappText(): ACTION ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    //SEND WHATSAPP AUDIO:
    fun sendWhatsappAudio(
        usable: LibraryItem?,
        lastRecording: String,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable!!.name
            if (lastRecording != "") {
                //Get audio file:
                val audioFile = File(recDir, lastRecording)   //Flac
                val uri =
                    FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", audioFile)

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
                ttsToRead = defaultReplies.replyWAVoiceSent(contactName)
                Log.d(TAG, "Whatsapp audio message ready to be sent!")
            } else {
                ttsToRead = defaultReplies.replyError()
                Log.w(TAG, "sendWhatsappAudio(): message not attached!")
            }
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendWhatsappAudio(): ACTION ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    // OPEN LINK:
    fun openLink(
        usable: LibraryItem?
    ): String {
        try {
            val urlToOpen = usable!!.url
            utils.openLink(context, url = urlToOpen, fromService = true)
        } catch (e: Exception) {
            Log.w(TAG, "openLink(): ACTION ERROR: ", e)
        }
        return ""
    }

}