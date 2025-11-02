package com.ftrono.DJames.be.models


import androidx.compose.runtime.Composable
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyPlayable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.objectbox.converter.PropertyConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer


// MODEL CLASSES:
@Serializable
data class HttpResponse(
    val code: Int,
    val body: String
)


data class RawLinkPreview(
    var title: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var url: String = ""
)


data class QuickAction(
    var description: String,
    var content: @Composable () -> Unit,
)


@Serializable
data class AiReply(
    val langCode: String,
    val text: String
)


@Serializable
data class DispatcherInfo(
    var lastRecording: String = "",   //Flac only
    var aiReplies: List<AiReply> = listOf(),
    var actionType: ActionType? = null, //"call", ""
    var end: Boolean = false,   //fulfillment complete
    var fail: Boolean = false,   //fulfillment complete
    var playAcknowledge: Boolean = false,   //play the acknowledge tone
    var followUp: Boolean = false,   //from 2nd query on
    var messageMode: Boolean = false,   //specific for messages only
    var messageType: String = "",
    var intentName: String = "Fallback",
    var reqLanguage: String = "",
    var playType: String = "",
    var contextType: String = "",
    var usable: LibraryItem = LibraryItem(),
    var playable: SpotifyPlayable = SpotifyPlayable(),
    var testV3: Boolean = false,
)


// ENUM:
enum class ActionType {
    PLAY, CALL, SMS, WA_TEXT, WA_VOICE, OPEN_URL
}


// CONVERTERS:
open class JsonConverter<T>(
    private val serializer: KSerializer<T>
) : PropertyConverter<T?, String> {

    override fun convertToEntityProperty(databaseValue: String?): T? {
        return databaseValue?.let { Json.decodeFromString(serializer, it) }
    }

    override fun convertToDatabaseValue(entityProperty: T?): String? {
        return entityProperty?.let { Json.encodeToString(serializer, it) }
    }
}


open class JsonListConverter<T>(
    serializer: KSerializer<T>
) : PropertyConverter<MutableList<T>?, String> {

    private val listSerializer = ListSerializer(serializer)

    override fun convertToEntityProperty(databaseValue: String?): MutableList<T>? {
        return databaseValue?.let {
            Json.decodeFromString(listSerializer, it)
        }?.toMutableList()
    }

    override fun convertToDatabaseValue(entityProperty: MutableList<T>?): String? {
        return entityProperty?.let {
            Json.encodeToString(listSerializer, it)
        }
    }
}


class ActionTypeConverter : PropertyConverter<ActionType, String> {
    override fun convertToDatabaseValue(entityProperty: ActionType?): String? {
        return entityProperty?.name
    }

    override fun convertToEntityProperty(databaseValue: String?): ActionType? {
        return databaseValue?.let { value ->
            ActionType.entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}


// Convert String to ActionType:
fun actionTypeFromString(value: String?): ActionType? {
    return ActionType.entries.find { it.name.equals(value, ignoreCase = true) }
}

// Convert ActionType to String:
fun actionTypeToString(actionType: ActionType?): String? {
    return actionType?.name
}
