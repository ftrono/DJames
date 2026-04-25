package com.ftrono.DJames.kaigraph.data

import com.ftrono.DJames.application.START
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.models.AiReply
import kotlinx.serialization.Serializable

// STATE:
@Serializable
data class StateInfo(
    // Agents:
    var messages: MutableList<ChatMessage> = mutableListOf(),
    var next: String = START,
    var interrupt: Boolean = false,
    var isSilence: Boolean = false,
    var fail: Boolean = false,   //fulfillment complete
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
    var messageMode: Boolean = false,   //specific for messages only
    var messageType: String = "",

    // Fulfillment:
    var intentName: String = "Fallback",
    var aiReplies: List<AiReply> = listOf(),

    // More:
    var reqLangCode: String = "en",   // Default
    var reqLangName: String = "english",   // Default
    var lastRecording: String = "",   //Flac only
    var playType: String = "",
    var contextType: String = "",
)