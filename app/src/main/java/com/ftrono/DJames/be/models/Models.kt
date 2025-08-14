package com.ftrono.DJames.be.models


import com.ftrono.DJames.be.database.ItemInfoUse
import com.ftrono.DJames.be.database.SpotifyPlayable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// MODEL CLASSES:
@Serializable
data class HttpResponse(
    val code: Int,
    val body: String
)

@Serializable
data class AiReply(
    val langCode: String,
    val text: String
)


// ENUM:
enum class ActionType {
    PLAY, CALL, SMS, WA_TEXT, WA_VOICE, OPEN_URL
}

// Convert String to ActionType:
fun actionTypeFromString(value: String?): ActionType? {
    return ActionType.entries.find { it.name.equals(value, ignoreCase = true) }
}

// Convert ActionType to String:
fun actionTypeToString(actionType: ActionType?): String? {
    return actionType?.name
}

@Serializable
data class DispatcherInfo(
    var aiReplies: List<AiReply> = listOf(),
    var actionType: ActionType? = null, //"call", ""
    var end: Boolean = false,   //fulfillment complete
    var fail: Boolean = false,   //fulfillment complete
    var playAcknowledge: Boolean = false,   //play the acknowledge tone
    var followUp: Boolean = false,   //generic
    var messageMode: Boolean = false,   //specific for messages only
    var messageType: String = "",
    var intentName: String = "Fallback",
    var reqLanguage: String = "",
    var reqPlayLinkName: String = "",
    var playType: String = "",
    var contextType: String = "",
    var usable: ItemInfoUse = ItemInfoUse(),
    var playable: SpotifyPlayable = SpotifyPlayable()
)
