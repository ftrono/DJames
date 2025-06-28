package com.ftrono.DJames.be.models

import com.ftrono.DJames.be.database.ItemInfoUse
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
data class DispatcherInfo(
    var end: Boolean = false,   //fulfillment complete
    var fail: Boolean = false,   //fulfillment complete
    var playAcknowledge: Boolean = false,   //play the acknowledge tone
    var followUp: Boolean = false,   //generic
    var messageMode: Boolean = false,   //specific for messages only
    var messageType: String = "",
    var toastText: String = "",
    var intentName: String = "Fallback",
    var reqLanguage: String = "",
    var playType: String = "",
    var contextType: String = "",
    var usable: ItemInfoUse = ItemInfoUse(),
)


