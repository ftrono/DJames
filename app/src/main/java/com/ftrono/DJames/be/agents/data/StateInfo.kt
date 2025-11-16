package com.ftrono.DJames.be.agents.data

import com.ftrono.DJames.application.START
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.models.AiReply
import kotlinx.serialization.Serializable


// STATE:
@Serializable
data class StateInfo(
    // Graph state:
    var messages: MutableList<ChatMessage> = mutableListOf(),
    var next: String = START,
    var interrupt: Boolean = false,
    var isSilence: Boolean = false,
    var fail: Boolean = false,   //fulfillment complete
    var end: Boolean = false,   //fulfillment complete
    var intentName: String = "Fallback",
    var lastUserMsgId: Long = 0L,

    // Modes:
    var noSave: Boolean = false,   // don't store message
    var messageMode: Boolean = false,   //specific for messages only
    var messageType: String = "",

    // Replies & Actions:
    var aiReplies: List<AiReply> = listOf(),
    var actionType: ActionType? = null, //"call", ""
    var playAcknowledge: Boolean = false,   //play the acknowledge tone

    // More:
    var reqLanguage: String = "",
    var lastRecording: String = "",   //Flac only
    var playType: String = "",
    var contextType: String = "",
    var usable: LibraryItem? = null,
    var playable: SpotifyPlayable? = null,
)
