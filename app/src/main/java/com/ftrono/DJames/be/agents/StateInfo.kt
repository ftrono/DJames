package com.ftrono.DJames.be.agents

import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.models.AiReply
import kotlinx.serialization.Serializable


// STATE:
@Serializable
data class StateInfo(
    var lastRecording: String = "",   //Flac only
    var aiReplies: List<AiReply> = listOf(),
    var actionType: ActionType? = null, //"call", ""
    var end: Boolean = false,   //fulfillment complete
    var fail: Boolean = false,   //fulfillment complete
    var noSave: Boolean = false,   // don't store message
    var playAcknowledge: Boolean = false,   //play the acknowledge tone
    var messageMode: Boolean = false,   //specific for messages only
    var messageType: String = "",
    var intentName: String = "Fallback",
    var agentName: String = "Fallback",
    var reqLanguage: String = "",
    var playType: String = "",
    var contextType: String = "",
    var usable: LibraryItem = LibraryItem(),
    var playable: SpotifyPlayable = SpotifyPlayable(),
)
