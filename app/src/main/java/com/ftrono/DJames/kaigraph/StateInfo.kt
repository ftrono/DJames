package com.ftrono.DJames.kaigraph

import com.ftrono.DJames.application.START
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.database.Attachments
import kotlinx.serialization.Serializable

// STATE:
@Serializable
data class StateInfo(
    // Agents:
    var messages: MutableList<ChatMessage> = mutableListOf(),
    var next: String = START,
    var interrupt: Boolean = false,
    var isSilence: Boolean = false,
    var fail: Boolean = false,
    var lastUserMsgId: Long = 0L,
    var fromVoice: Boolean = false,
    var isStart: Boolean = false,
    var attachments: Attachments = Attachments(),

    // Replies & Actions:
    var fullReply: String = "",
    var actionType: ActionType? = null,   //"call", ""
    var playAcknowledge: Boolean = false,   //play the acknowledge tone

    // Modes:
    var noSave: Boolean = false,   // don't store message
    var voiceMessageMode: Boolean = false,   // record Whatsapp voice message

    // More:
    var agentName: String = "",
    var lastRecording: String = "",   //Flac only
    var reqLangCode: String = "en",   // Default
)