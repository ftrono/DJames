package com.ftrono.DJames.be.agents.chat

import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import android.net.Uri
import com.ftrono.DJames.BuildConfig
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.recDir
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.kaigraph.StateInfo
import com.ftrono.DJames.be.database.ActionType
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
        fromOldChat: Boolean = false,
        lastRecording: String = "",
        forceExecute: Boolean = false,
    ): String {
        return when (action) {
            // Deferred (after TTS):
            ActionType.PLAY -> spotifyPlay(playable)
            ActionType.CALL -> makeCall(usable, fromOldChat)
            // Immediate (before TTS):
            ActionType.SMS -> if (forceExecute) sendSMS(text, usable) else ""
            ActionType.WA_TEXT -> if (forceExecute) sendWhatsappText(text, usable) else ""
            ActionType.WA_VOICE -> sendWhatsappAudio(lastRecording)
            ActionType.OPEN_URL -> if (forceExecute || fromOldChat) openLink(usable) else ""
        }
    }

    // MAIN: EXECUTOR:
    fun execute(
        latestState: StateInfo
    ): String {
        // Execute action:
        val updReply = if (latestState.actionType == null) "" else {
            launchAction(
                action = latestState.actionType!!,
                text = latestState.messages.last().content,
                usable = latestState.attachments.usable,
                playable = latestState.attachments.spotifyPlay,
                lastRecording = latestState.lastRecording
            )
        }
        return updReply
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
            return defaultReplies.replyCalling(usable.name, contactPhone)
        } catch (e: Exception) {
            Log.w(TAG, "makeCall(): ACTION ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    //SEND SMS:
    fun sendSMS(
        text: String,
        usable: LibraryItem?,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable!!.name
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            var messageText = "[DJ] $text"
            val smsManager: SmsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(messageText)
            smsManager.sendMultipartTextMessage(contactPhone, null, parts, null, null)
            ttsToRead = defaultReplies.replySMSSent(contactName)
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
        usable: LibraryItem? = null,
    ): String {
        try {
            var ttsToRead = ""
            val contactName = usable!!.name
            val phoneSet = usable.phoneSet!!
            val contactPhone = "${phoneSet.prefix}${phoneSet.phone}"
            var messageText = "[DJ] $text"

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
            ttsToRead = defaultReplies.replyWATextReady(contactName)
            Log.d(TAG, "Whatsapp text message ready to be sent!")
            return ttsToRead

        } catch (e: Exception) {
            Log.w(TAG, "sendWhatsappText(): ACTION ERROR: ", e)
            return defaultReplies.replyError()
        }
    }

    //SEND WHATSAPP AUDIO:
    fun sendWhatsappAudio(
        lastRecording: String,
    ): String {
        try {
            var ttsToRead = ""
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
                ttsToRead = defaultReplies.replyWAVoiceReady()
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