package com.ftrono.DJames.be.chat

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
import com.ftrono.DJames.application.recDir
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.models.ActionType
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.spotify.SpotifyPlayer
import java.io.File


class ActionsExecutor(
    private val context: Context
) {
    private val TAG = ActionsExecutor::class.java.simpleName
    private val chatManager = ChatManager(context)

    // HELPER: LAUNCHER ONLY:
    fun launchAction(
        action: ActionType,
        usable: LibraryItem? = null,
        playable: SpotifyPlayable? = null,
        reqLanguage: String = "",
        fromOldChat: Boolean = false,
        lastRecording: String = "",
    ): String {
        return when (action) {
            ActionType.PLAY -> spotifyPlay(playable!!)
            ActionType.CALL -> makeCall(usable!!, fromOldChat)
            ActionType.SMS -> sendSMS(usable!!, reqLanguage, fromOldChat)
            ActionType.WA_TEXT -> sendWhatsappText(usable!!, reqLanguage, fromOldChat)
            ActionType.WA_VOICE -> sendWhatsappAudio(usable!!, lastRecording, fromOldChat)
            ActionType.OPEN_URL -> openLink(usable!!)
        }
    }

    // MAIN: EXECUTOR:
    fun execute(
        latestDispatch: DispatcherInfo
    ): List<AiReply> {
        var updatedReplies = listOf<AiReply>()
        // Execute action:
        var stringReply = if (latestDispatch.actionType == null) "" else {
            launchAction(
                action = latestDispatch.actionType!!,
                usable = latestDispatch.usable,
                playable = latestDispatch.playable,
                reqLanguage = latestDispatch.reqLanguage,
                lastRecording = latestDispatch.lastRecording
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
        playable: SpotifyPlayable
    ): String {
        val spotifyPlayer = SpotifyPlayer(context)
        spotifyPlayer.spotifyPlay(playable)
        return ""
    }

    //CALL:
    fun makeCall(
        usable: LibraryItem,
        fromOldChat: Boolean = false,
    ): String {
        try {
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            utils.makeCall(context, contactPhone, fromService = !fromOldChat)
        } catch (e: Exception) {
            Log.w(TAG, "makeCall(): TOOL ERROR: ", e)
        }
        return ""
    }

    //SEND SMS:
    fun sendSMS(
        usable: LibraryItem,
        reqLanguage: String,
        fromOldChat: Boolean = false,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable.name
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            if (fromOldChat) {
                // START NEW:
                val curText = "Send an SMS to $contactName"
                chatManager.processQuery(curText, restart = true)
            } else {
                // SEND:
                var messageText = "[DJ] ${
                    fulfillmentUtils.replaceEmojis(
                        context = context,
                        text = nlp_queryText,
                        reqLanguage = reqLanguage
                    )
                }"
                val smsManager: SmsManager = SmsManager.getDefault()
                val parts = smsManager.divideMessage(messageText)
                smsManager.sendMultipartTextMessage(contactPhone, null, parts, null, null)
                ttsToRead = defaultReplies.replySmsSent(contactName)
                Log.d(TAG, ttsToRead)
            }
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendSMS(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    //SEND WHATSAPP TEXT:
    fun sendWhatsappText(
        usable: LibraryItem,
        reqLanguage: String,
        fromOldChat: Boolean = false,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable.name
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            if (fromOldChat) {
                // START NEW:
                val curText = "Send a Whatsapp message to $contactName"
                chatManager.processQuery(curText, restart = true)
            } else {
                // SEND:
                var messageText = "[DJ] ${
                    fulfillmentUtils.replaceEmojis(
                        context = context,
                        text = nlp_queryText,
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
            }
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendWhatsappText(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    //SEND WHATSAPP AUDIO:
    fun sendWhatsappAudio(
        usable: LibraryItem,
        lastRecording: String,
        fromOldChat: Boolean = false,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable.name
            if (fromOldChat) {
                // START NEW:
                val curText = "Send a Whatsapp voice message to $contactName"
                chatManager.processQuery(curText, restart = true)
            } else if (lastRecording != "") {
                //Get audio file:
                val audioFile = File(recDir, lastRecording)   //Flac
                val uri =
                    FileProvider.getUriForFile(context, "com.ftrono.DJames.provider", audioFile)

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
            Log.w(TAG, "sendWhatsappAudio(): TOOL ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    // OPEN LINK:
    fun openLink(
        usable: LibraryItem
    ): String {
        val urlToOpen = usable.url
        utils.openLink(context, url = urlToOpen, fromService = true)
        return ""
    }

}