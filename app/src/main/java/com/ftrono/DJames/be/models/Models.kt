package com.ftrono.DJames.be.models


import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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


data class GuideText(
    var intro: String = "",
    var content: String = "",
    var outro: String = ""
)


data class Coordinates(
    var x: Int,
    var y: Int
)


data class SelectorItem(
    var id: String = "",
    var title: String = "",
    var iconVector: ImageVector? = null,
    var iconPainter: Painter? = null,
    var color: Color? = null,
    var colorBackground: Color? = null,
    var useImage: Boolean = false,
    var disableGray: Boolean = false,
    var useCustomClick: Boolean = false,
    var onClick: () -> Unit = {},
)


data class QuickAction(
    var id: String,
    var title: String,
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