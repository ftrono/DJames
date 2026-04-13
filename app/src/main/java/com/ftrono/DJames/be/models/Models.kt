package com.ftrono.DJames.be.models


import androidx.compose.runtime.Composable
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


@Serializable
data class TTSVoiceSettings(
    val speed: Double,
    val stability: Double,
    val similarity_boost: Double,
)

@Serializable
data class TTSRequest(
    val text: String,
    val model_id: String,
    val voice_settings: TTSVoiceSettings
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


data class ZipBackup(
    var jsonPrefs: String = "",
    var jsonLibrary: String = "",
)


data class RecDetails(
    var recName: String = "",
    var recPath: String = "",
    var speechPct: Int = 0
)


@Serializable
data class AiReply(
    var langCode: String,
    var text: String
)


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


abstract class EnumTypeConverter<T : Enum<T>>(
    private val enumValues: Array<T>
) : PropertyConverter<T, String> {

    override fun convertToDatabaseValue(entityProperty: T?): String? {
        return entityProperty?.name
    }

    override fun convertToEntityProperty(databaseValue: String?): T? {
        return databaseValue?.let { value ->
            enumValues.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}